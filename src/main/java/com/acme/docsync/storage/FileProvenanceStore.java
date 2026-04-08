package com.acme.docsync.storage;

import com.acme.docsync.model.IngestionValidationException;
import com.acme.docsync.model.ProvenanceRecord;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

/**
 * Local file-backed provenance store.
 */
public final class FileProvenanceStore implements ProvenanceStore {
  private final Path baseDirectory;

  /**
   * Creates a provenance store.
   *
   * @param baseDirectory base state directory
   */
  public FileProvenanceStore(Path baseDirectory) {
    this.baseDirectory = Objects.requireNonNull(
        baseDirectory, "baseDirectory is required");
  }

  @Override
  public synchronized void put(ProvenanceRecord record) {
    Objects.requireNonNull(record, "record is required");
    try {
      Files.createDirectories(baseDirectory);
      persist(sourceFile(record.tenantId(), record.sourcePath()), record);
      persist(canonicalFile(record.tenantId(), record.canonicalPath()), record);
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist provenance record");
    }
  }

  @Override
  public synchronized ProvenanceRecord findBySource(String tenantId, String sourcePath) {
    return load(sourceFile(tenantId, sourcePath));
  }

  @Override
  public synchronized ProvenanceRecord findByCanonical(
      String tenantId, String canonicalPath) {
    return load(canonicalFile(tenantId, canonicalPath));
  }

  private void persist(Path file, ProvenanceRecord record) throws IOException {
    Properties properties = new Properties();
    properties.setProperty("tenantId", record.tenantId());
    properties.setProperty("sourcePath", record.sourcePath());
    properties.setProperty("canonicalPath", record.canonicalPath());
    properties.setProperty("sourceVersion", record.sourceVersion() == null
        ? "" : record.sourceVersion());
    properties.setProperty("updatedAt", record.updatedAt().toString());
    try (OutputStream output = Files.newOutputStream(file)) {
      properties.store(output, "provenance record");
    }
  }

  private ProvenanceRecord load(Path file) {
    if (!Files.exists(file)) {
      return null;
    }
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(file)) {
      properties.load(input);
      String sourceVersion = properties.getProperty("sourceVersion");
      return new ProvenanceRecord(
          properties.getProperty("tenantId"),
          properties.getProperty("sourcePath"),
          properties.getProperty("canonicalPath"),
          sourceVersion == null || sourceVersion.isBlank() ? null : sourceVersion,
          Instant.parse(properties.getProperty("updatedAt")));
    } catch (IOException | RuntimeException e) {
      throw new IngestionValidationException("failed to load provenance record");
    }
  }

  private Path sourceFile(String tenantId, String sourcePath) {
    return baseDirectory.resolve("source_" + encode(tenantId + "|" + sourcePath) + ".properties");
  }

  private Path canonicalFile(String tenantId, String canonicalPath) {
    return baseDirectory.resolve("canonical_" + encode(tenantId + "|" + canonicalPath)
        + ".properties");
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
