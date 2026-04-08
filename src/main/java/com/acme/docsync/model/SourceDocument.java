package com.acme.docsync.model;

import java.util.Objects;

/**
 * Source document payload and metadata before parsing.
 *
 * @param tenantId tenant identifier
 * @param sourcePath tenant-scoped source path
 * @param mediaType media type when known
 * @param payload raw bytes
 */
public record SourceDocument(
    String tenantId,
    String sourcePath,
    String mediaType,
    byte[] payload
) {
  public SourceDocument {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(sourcePath, "sourcePath is required");
    Objects.requireNonNull(payload, "payload is required");
    payload = payload.clone();
  }
}
