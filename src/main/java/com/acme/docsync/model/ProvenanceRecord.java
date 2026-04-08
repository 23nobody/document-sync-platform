package com.acme.docsync.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Provenance mapping from source to canonical path.
 *
 * @param tenantId tenant identifier
 * @param sourcePath source path
 * @param canonicalPath canonical path
 * @param sourceVersion optional source version
 * @param updatedAt mapping update time
 */
public record ProvenanceRecord(
    String tenantId,
    String sourcePath,
    String canonicalPath,
    String sourceVersion,
    Instant updatedAt
) {
  public ProvenanceRecord {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(sourcePath, "sourcePath is required");
    Objects.requireNonNull(canonicalPath, "canonicalPath is required");
    Objects.requireNonNull(updatedAt, "updatedAt is required");
  }
}
