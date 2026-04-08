package com.acme.docsync.storage;

import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.model.IngestionValidationException;
import com.acme.docsync.model.TombstoneRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * File-backed tombstone store.
 */
public final class FileTombstoneStore implements TombstoneStore {
  private final Path baseDirectory;
  private final AtomicFileWriter atomicFileWriter;

  public FileTombstoneStore(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    this.atomicFileWriter = new AtomicFileWriter();
  }

  @Override
  public synchronized void put(TombstoneRecord record) {
    try {
      Path file = file(record.tenantId(), record.canonicalPath());
      Files.createDirectories(file.getParent());
      Properties props = new Properties();
      props.setProperty("tenantId", record.tenantId());
      props.setProperty("canonicalPath", record.canonicalPath());
      props.setProperty("policy", record.policy().name());
      props.setProperty("deletedAt", record.deletedAt().toString());
      atomicFileWriter.write(file, output -> {
        try {
          props.store(output, "tombstone");
        } catch (IOException e) {
          throw new IngestionValidationException("failed to write tombstone");
        }
      });
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist tombstone");
    }
  }

  @Override
  public synchronized TombstoneRecord find(String tenantId, String canonicalPath) {
    Path file = file(tenantId, canonicalPath);
    if (!Files.exists(file)) {
      return null;
    }
    Properties props = new Properties();
    try {
      props.load(Files.newInputStream(file));
      return new TombstoneRecord(
          props.getProperty("tenantId"),
          props.getProperty("canonicalPath"),
          DeletePolicy.valueOf(props.getProperty("policy")),
          Instant.parse(props.getProperty("deletedAt")));
    } catch (IOException e) {
      throw new IngestionValidationException("failed to load tombstone");
    }
  }

  private Path file(String tenantId, String canonicalPath) {
    String safe = canonicalPath.replace('/', '_');
    return baseDirectory.resolve(tenantId).resolve(safe + ".properties");
  }
}
