package com.acme.docsync.storage;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * File-backed canonical document store.
 */
public final class FileCanonicalDocumentStore implements CanonicalDocumentStore {
  private final Path baseDirectory;
  private final AtomicFileWriter atomicFileWriter;

  public FileCanonicalDocumentStore(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    this.atomicFileWriter = new AtomicFileWriter();
  }

  @Override
  public synchronized void put(CanonicalDocument document) {
    try {
      Path file = file(document.tenantId(), document.canonicalPath());
      Files.createDirectories(file.getParent());
      Properties props = new Properties();
      props.setProperty("tenantId", document.tenantId());
      props.setProperty("canonicalPath", document.canonicalPath());
      props.setProperty("fields", document.fields().toString());
      atomicFileWriter.write(file, output -> {
        try {
          props.store(output, "canonical doc");
        } catch (IOException e) {
          throw new IngestionValidationException("failed to write canonical document");
        }
      });
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist canonical document");
    }
  }

  @Override
  public synchronized CanonicalDocument get(String tenantId, String canonicalPath) {
    Path file = file(tenantId, canonicalPath);
    if (!Files.exists(file)) {
      return null;
    }
    Properties props = new Properties();
    try {
      props.load(Files.newInputStream(file));
      return new CanonicalDocument(
          props.getProperty("tenantId"),
          props.getProperty("canonicalPath"),
          Map.of("raw", props.getProperty("fields", "{}")));
    } catch (IOException e) {
      throw new IngestionValidationException("failed to load canonical document");
    }
  }

  @Override
  public synchronized void delete(String tenantId, String canonicalPath, DeletePolicy policy) {
    Path file = file(tenantId, canonicalPath);
    if (policy == DeletePolicy.TOMBSTONE_AND_PHYSICAL_DELETE) {
      try {
        Files.deleteIfExists(file);
      } catch (IOException e) {
        throw new IngestionValidationException("failed to delete canonical document");
      }
    }
  }

  private Path file(String tenantId, String canonicalPath) {
    String safe = canonicalPath.replace('/', '_');
    return baseDirectory.resolve(tenantId).resolve(safe + ".properties");
  }
}
