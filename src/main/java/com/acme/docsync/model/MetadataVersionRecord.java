package com.acme.docsync.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata profile version record for auditing.
 *
 * @param version version number
 * @param fingerprint fingerprint for the version
 * @param updatedAt version timestamp
 */
public record MetadataVersionRecord(
    int version,
    FormatFingerprint fingerprint,
    Instant updatedAt
) {
  public MetadataVersionRecord {
    if (version <= 0) {
      throw new IllegalArgumentException("version must be greater than zero");
    }
    Objects.requireNonNull(fingerprint, "fingerprint is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
  }
}
