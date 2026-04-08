package com.acme.docsync.metadata;

import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.FormatFingerprint;
import com.acme.docsync.model.IngestionValidationException;
import com.acme.docsync.model.MetadataVersionRecord;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * File-backed metadata registry with version history.
 */
public final class FileMetadataRegistry implements MetadataRegistry {
  private final Path baseDirectory;
  private final ConcurrentMap<String, DirectoryMetadataProfile> latestByKey;
  private final ConcurrentMap<String, List<MetadataVersionRecord>> historyByKey;

  /**
   * Creates a file-backed metadata registry.
   *
   * @param baseDirectory base metadata state directory
   */
  public FileMetadataRegistry(Path baseDirectory) {
    this.baseDirectory = Objects.requireNonNull(
        baseDirectory, "baseDirectory is required");
    this.latestByKey = new ConcurrentHashMap<>();
    this.historyByKey = new ConcurrentHashMap<>();
  }

  @Override
  public synchronized void upsertProfile(DirectoryMetadataProfile profile) {
    Objects.requireNonNull(profile, "profile is required");
    String key = key(profile.tenantId(), profile.directoryPath());
    DirectoryMetadataProfile existing = latestByKey.get(key);
    DirectoryMetadataProfile next = withNextVersion(existing, profile);
    latestByKey.put(key, next);
    historyByKey.computeIfAbsent(key, ignored -> new ArrayList<>());
    if (existing == null
        || !existing.fingerprint().value().equals(next.fingerprint().value())) {
      historyByKey.get(key).add(new MetadataVersionRecord(
          next.version(), next.fingerprint(), next.updatedAt()));
    }
    persist(key, next, historyByKey.get(key));
  }

  @Override
  public synchronized DirectoryMetadataProfile getLatest(
      String tenantId, String directoryPath) {
    String key = key(tenantId, directoryPath);
    DirectoryMetadataProfile cached = latestByKey.get(key);
    if (cached != null) {
      return cached;
    }
    load(key);
    return latestByKey.get(key);
  }

  @Override
  public synchronized List<MetadataVersionRecord> history(
      String tenantId, String directoryPath) {
    String key = key(tenantId, directoryPath);
    if (!historyByKey.containsKey(key)) {
      load(key);
    }
    List<MetadataVersionRecord> records = historyByKey.getOrDefault(
        key, List.of());
    return Collections.unmodifiableList(records);
  }

  private DirectoryMetadataProfile withNextVersion(
      DirectoryMetadataProfile existing, DirectoryMetadataProfile incoming) {
    if (existing == null) {
      return incoming;
    }
    boolean changed = !existing.fingerprint().value().equals(
        incoming.fingerprint().value());
    int nextVersion = changed ? existing.version() + 1 : existing.version();
    return new DirectoryMetadataProfile(
        incoming.tenantId(),
        incoming.directoryPath(),
        incoming.encoding(),
        incoming.mediaType(),
        incoming.schema(),
        incoming.schemaVersion(),
        incoming.parserHints(),
        incoming.fingerprint(),
        nextVersion,
        incoming.updatedAt());
  }

  private void persist(
      String key,
      DirectoryMetadataProfile profile,
      List<MetadataVersionRecord> history) {
    Path latestFile = latestFile(key);
    Path historyFile = historyFile(key);
    try {
      Files.createDirectories(baseDirectory);
      Properties latest = new Properties();
      latest.setProperty("tenantId", profile.tenantId());
      latest.setProperty("directoryPath", profile.directoryPath());
      latest.setProperty("encoding", profile.encoding());
      latest.setProperty("mediaType", profile.mediaType());
      latest.setProperty("schema", profile.schema());
      latest.setProperty("schemaVersion", profile.schemaVersion());
      latest.setProperty("fingerprint", profile.fingerprint().value());
      latest.setProperty("algorithm", profile.fingerprint().algorithm());
      latest.setProperty("version", Integer.toString(profile.version()));
      latest.setProperty("updatedAt", profile.updatedAt().toString());
      latest.setProperty("parserHints", flattenHints(profile.parserHints()));
      try (OutputStream output = Files.newOutputStream(latestFile)) {
        latest.store(output, "metadata latest");
      }

      Properties historyProps = new Properties();
      for (int i = 0; i < history.size(); i++) {
        MetadataVersionRecord record = history.get(i);
        historyProps.setProperty(i + ".version", Integer.toString(record.version()));
        historyProps.setProperty(i + ".fingerprint", record.fingerprint().value());
        historyProps.setProperty(i + ".algorithm", record.fingerprint().algorithm());
        historyProps.setProperty(i + ".updatedAt", record.updatedAt().toString());
      }
      try (OutputStream output = Files.newOutputStream(historyFile)) {
        historyProps.store(output, "metadata history");
      }
    } catch (IOException e) {
      throw new IngestionValidationException("failed to persist metadata profile");
    }
  }

  private void load(String key) {
    Path latestFile = latestFile(key);
    Path historyFile = historyFile(key);
    if (!Files.exists(latestFile)) {
      return;
    }
    try {
      Properties latest = new Properties();
      try (InputStream input = Files.newInputStream(latestFile)) {
        latest.load(input);
      }
      DirectoryMetadataProfile profile = new DirectoryMetadataProfile(
          latest.getProperty("tenantId"),
          latest.getProperty("directoryPath"),
          latest.getProperty("encoding"),
          latest.getProperty("mediaType"),
          latest.getProperty("schema"),
          latest.getProperty("schemaVersion"),
          parseHints(latest.getProperty("parserHints", "")),
          new FormatFingerprint(
              latest.getProperty("fingerprint"),
              latest.getProperty("algorithm", "SHA-256")),
          Integer.parseInt(latest.getProperty("version")),
          Instant.parse(latest.getProperty("updatedAt")));
      latestByKey.put(key, profile);

      List<MetadataVersionRecord> loadedHistory = new ArrayList<>();
      if (Files.exists(historyFile)) {
        Properties historyProps = new Properties();
        try (InputStream input = Files.newInputStream(historyFile)) {
          historyProps.load(input);
        }
        for (int i = 0; ; i++) {
          String version = historyProps.getProperty(i + ".version");
          if (version == null) {
            break;
          }
          loadedHistory.add(new MetadataVersionRecord(
              Integer.parseInt(version),
              new FormatFingerprint(
                  historyProps.getProperty(i + ".fingerprint"),
                  historyProps.getProperty(i + ".algorithm")),
              Instant.parse(historyProps.getProperty(i + ".updatedAt"))));
        }
      }
      historyByKey.put(key, loadedHistory);
    } catch (IOException | RuntimeException e) {
      throw new IngestionValidationException("failed to load metadata profile");
    }
  }

  private String key(String tenantId, String directoryPath) {
    if (tenantId == null || directoryPath == null) {
      throw new IngestionValidationException("tenantId and directoryPath are required");
    }
    return tenantId + "|" + directoryPath;
  }

  private Path latestFile(String key) {
    return baseDirectory.resolve(fileName(key) + ".latest.properties");
  }

  private Path historyFile(String key) {
    return baseDirectory.resolve(fileName(key) + ".history.properties");
  }

  private String fileName(String key) {
    return URLEncoder.encode(key, StandardCharsets.UTF_8);
  }

  private String flattenHints(Map<String, String> hints) {
    StringBuilder builder = new StringBuilder();
    Map<String, String> sorted = new HashMap<>(hints);
    sorted.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> builder.append(entry.getKey())
            .append('=').append(entry.getValue()).append(';'));
    return builder.toString();
  }

  private Map<String, String> parseHints(String raw) {
    Map<String, String> hints = new HashMap<>();
    if (raw == null || raw.isBlank()) {
      return hints;
    }
    String[] tokens = raw.split(";");
    for (String token : tokens) {
      if (token.isBlank() || !token.contains("=")) {
        continue;
      }
      int index = token.indexOf('=');
      hints.put(token.substring(0, index), token.substring(index + 1));
    }
    return hints;
  }
}
