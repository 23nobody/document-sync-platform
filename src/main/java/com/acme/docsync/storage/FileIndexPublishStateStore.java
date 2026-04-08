package com.acme.docsync.storage;

import com.acme.docsync.model.IngestionValidationException;
import com.acme.docsync.model.PublishStateRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

/**
 * File-backed index publish state store.
 */
public final class FileIndexPublishStateStore implements IndexPublishStateStore {
  private final Path baseDirectory;
  private final AtomicFileWriter atomicFileWriter;

  public FileIndexPublishStateStore(Path baseDirectory) {
    this.baseDirectory = baseDirectory;
    this.atomicFileWriter = new AtomicFileWriter();
  }

  @Override
  public synchronized void upsert(PublishStateRecord record) {
    try {
      Path file = file(record.tenantId(), record.canonicalPath());
      Files.createDirectories(file.getParent());
      Properties props = new Properties();
      props.setProperty("tenantId", record.tenantId());
      props.setProperty("canonicalPath", record.canonicalPath());
      props.setProperty("checksum", record.checksum());
      props.setProperty("publishedAt", record.publishedAt().toString());
      props.setProperty("deleted", Boolean.toString(record.deleted()));
      atomicFileWriter.write(file, output -> {
        try {
          props.store(output, "publish state");
        } catch (IOException e) {
          throw new IngestionValidationException("failed to write publish state");
        }
      });
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist publish state");
    }
  }

  @Override
  public synchronized PublishStateRecord find(String tenantId, String canonicalPath) {
    Path file = file(tenantId, canonicalPath);
    if (!Files.exists(file)) {
      return null;
    }
    Properties props = new Properties();
    try {
      props.load(Files.newInputStream(file));
      return new PublishStateRecord(
          props.getProperty("tenantId"),
          props.getProperty("canonicalPath"),
          props.getProperty("checksum"),
          Instant.parse(props.getProperty("publishedAt")),
          Boolean.parseBoolean(props.getProperty("deleted")));
    } catch (IOException e) {
      throw new IngestionValidationException("failed to load publish state");
    }
  }

  @Override
  public synchronized void markDeleted(String tenantId, String canonicalPath, Instant at) {
    PublishStateRecord existing = find(tenantId, canonicalPath);
    if (existing == null) {
      upsert(new PublishStateRecord(tenantId, canonicalPath, "deleted", at, true));
      return;
    }
    upsert(new PublishStateRecord(
        existing.tenantId(),
        existing.canonicalPath(),
        existing.checksum(),
        at,
        true));
  }

  private Path file(String tenantId, String canonicalPath) {
    String safe = canonicalPath.replace('/', '_');
    return baseDirectory.resolve(tenantId).resolve(safe + ".properties");
  }
}
