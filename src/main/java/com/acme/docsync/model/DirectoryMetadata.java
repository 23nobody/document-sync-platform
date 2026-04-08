package com.acme.docsync.model;

import java.util.Objects;

/**
 * Directory-level metadata for format and encoding details.
 */
public record DirectoryMetadata(
    String tenantId,
    String directoryPath,
    String encoding,
    String mediaType,
    String formatFingerprint,
    String schemaVersion
) {
  public DirectoryMetadata {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(directoryPath, "directoryPath is required");
    Objects.requireNonNull(encoding, "encoding is required");
    Objects.requireNonNull(mediaType, "mediaType is required");
    Objects.requireNonNull(formatFingerprint, "formatFingerprint is required");
    Objects.requireNonNull(schemaVersion, "schemaVersion is required");
  }
}
