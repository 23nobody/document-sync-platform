package com.acme.docsync.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned directory metadata profile.
 *
 * @param tenantId tenant identifier
 * @param directoryPath directory path
 * @param encoding source encoding
 * @param mediaType media type
 * @param schema schema identifier
 * @param schemaVersion schema version
 * @param parserHints parser hints map
 * @param fingerprint computed format fingerprint
 * @param version profile version
 * @param updatedAt last update timestamp
 */
public record DirectoryMetadataProfile(
    String tenantId,
    String directoryPath,
    String encoding,
    String mediaType,
    String schema,
    String schemaVersion,
    Map<String, String> parserHints,
    FormatFingerprint fingerprint,
    int version,
    Instant updatedAt
) {
  public DirectoryMetadataProfile {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(directoryPath, "directoryPath is required");
    Objects.requireNonNull(encoding, "encoding is required");
    Objects.requireNonNull(mediaType, "mediaType is required");
    Objects.requireNonNull(schema, "schema is required");
    Objects.requireNonNull(schemaVersion, "schemaVersion is required");
    Objects.requireNonNull(parserHints, "parserHints is required");
    Objects.requireNonNull(fingerprint, "fingerprint is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
    if (version <= 0) {
      throw new IllegalArgumentException("version must be greater than zero");
    }
    parserHints = Map.copyOf(parserHints);
  }
}
