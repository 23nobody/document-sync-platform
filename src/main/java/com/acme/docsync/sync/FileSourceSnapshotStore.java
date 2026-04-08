package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Property-file backed source snapshot store.
 */
public final class FileSourceSnapshotStore implements SourceSnapshotStore {
  private final Path baseDirectory;

  /**
   * Creates a file-backed snapshot store.
   *
   * @param baseDirectory state directory
   */
  public FileSourceSnapshotStore(Path baseDirectory) {
    this.baseDirectory = Objects.requireNonNull(
        baseDirectory, "baseDirectory is required");
  }

  @Override
  public SourceSnapshot load(String tenantId, String sourceId) {
    Path snapshotFile = snapshotFile(tenantId, sourceId);
    if (!Files.exists(snapshotFile)) {
      return null;
    }
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(snapshotFile)) {
      properties.load(input);
      Map<String, Instant> filesByPath = new HashMap<>();
      for (String key : properties.stringPropertyNames()) {
        if ("capturedAt".equals(key)) {
          continue;
        }
        filesByPath.put(key, Instant.parse(properties.getProperty(key)));
      }
      Instant capturedAt = Instant.parse(properties.getProperty("capturedAt"));
      return new SourceSnapshot(tenantId, sourceId, capturedAt, filesByPath);
    } catch (IOException | RuntimeException e) {
      throw new IngestionValidationException("failed to load source snapshot");
    }
  }

  @Override
  public void persist(SourceSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot is required");
    Properties properties = new Properties();
    properties.setProperty("capturedAt", snapshot.capturedAt().toString());
    snapshot.filesByPath().forEach((key, value) ->
        properties.setProperty(key, value.toString()));
    Path snapshotFile = snapshotFile(snapshot.tenantId(), snapshot.sourceId());
    try {
      Path parent = snapshotFile.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      try (OutputStream output = Files.newOutputStream(snapshotFile)) {
        properties.store(output, "source snapshot");
      }
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist source snapshot");
    }
  }

  private Path snapshotFile(String tenantId, String sourceId) {
    return baseDirectory.resolve(tenantId + "_" + sourceId + ".properties");
  }
}
