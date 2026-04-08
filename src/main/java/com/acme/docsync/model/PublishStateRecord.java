package com.acme.docsync.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Publish state used by local index consumers.
 *
 * @param tenantId tenant identifier
 * @param canonicalPath canonical path
 * @param checksum canonical checksum
 * @param publishedAt publish timestamp
 * @param deleted whether deleted
 */
public record PublishStateRecord(
    String tenantId,
    String canonicalPath,
    String checksum,
    Instant publishedAt,
    boolean deleted
) {
  public PublishStateRecord {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(canonicalPath, "canonicalPath is required");
    Objects.requireNonNull(checksum, "checksum is required");
    Objects.requireNonNull(publishedAt, "publishedAt is required");
  }
}
