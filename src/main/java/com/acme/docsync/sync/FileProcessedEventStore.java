package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Property-file based processed event state store.
 */
public final class FileProcessedEventStore implements ProcessedEventStore {
  private final Path stateFile;
  private final ConcurrentMap<String, Instant> processedByKey;

  /**
   * Creates a file-backed processed event store.
   *
   * @param stateFile state file path
   */
  public FileProcessedEventStore(Path stateFile) {
    this.stateFile = Objects.requireNonNull(stateFile, "stateFile is required");
    this.processedByKey = new ConcurrentHashMap<>();
    load();
  }

  @Override
  public void markProcessed(IngestionContract event) {
    Objects.requireNonNull(event, "event is required");
    if (event.timestamp() == null) {
      throw new IngestionValidationException("timestamp is required");
    }
    String key = key(event.tenantId(), event.path());
    processedByKey.put(key, event.timestamp());
    persist();
  }

  @Override
  public boolean wasRecentlyProcessed(
      String tenantId, String path, Instant timestamp) {
    if (tenantId == null || path == null || timestamp == null) {
      return false;
    }
    Instant stored = processedByKey.get(key(tenantId, path));
    return timestamp.equals(stored);
  }

  @Override
  public Instant lastProcessedAt(String tenantId, String path) {
    if (tenantId == null || path == null) {
      return null;
    }
    return processedByKey.get(key(tenantId, path));
  }

  private String key(String tenantId, String path) {
    return tenantId + "|" + path;
  }

  private void load() {
    if (!Files.exists(stateFile)) {
      return;
    }
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(stateFile)) {
      properties.load(input);
      for (String name : properties.stringPropertyNames()) {
        processedByKey.put(name, Instant.parse(properties.getProperty(name)));
      }
    } catch (IOException | RuntimeException e) {
      throw new IngestionValidationException(
          "failed to load processed event store: " + stateFile);
    }
  }

  private synchronized void persist() {
    Properties properties = new Properties();
    processedByKey.forEach((key, value) -> properties.setProperty(
        key, value.toString()));
    try {
      Path parent = stateFile.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }
      try (OutputStream output = Files.newOutputStream(stateFile)) {
        properties.store(output, "processed event state");
      }
    } catch (IOException e) {
      throw new IngestionValidationException(
          "failed to persist processed event store: " + stateFile);
    }
  }
}
