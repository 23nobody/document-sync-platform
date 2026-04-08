package com.acme.docsync.model;

import java.util.Map;
import java.util.Objects;

/**
 * Canonical output document representation.
 *
 * @param tenantId tenant identifier
 * @param canonicalPath canonical path
 * @param fields canonical fields
 */
public record CanonicalDocument(
    String tenantId,
    String canonicalPath,
    Map<String, Object> fields
) {
  public CanonicalDocument {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(canonicalPath, "canonicalPath is required");
    Objects.requireNonNull(fields, "fields is required");
    fields = Map.copyOf(fields);
  }
}
