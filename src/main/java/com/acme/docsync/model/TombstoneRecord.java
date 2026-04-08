package com.acme.docsync.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Tombstone record for deleted canonical documents.
 *
 * @param tenantId tenant identifier
 * @param canonicalPath canonical path
 * @param policy applied delete policy
 * @param deletedAt delete timestamp
 */
public record TombstoneRecord(
    String tenantId,
    String canonicalPath,
    DeletePolicy policy,
    Instant deletedAt
) {
  public TombstoneRecord {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(canonicalPath, "canonicalPath is required");
    Objects.requireNonNull(policy, "policy is required");
    Objects.requireNonNull(deletedAt, "deletedAt is required");
  }
}
