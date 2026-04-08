package com.acme.docsync.model;

import java.util.Map;
import java.util.Objects;

/**
 * Parsed intermediate document representation.
 *
 * @param fields parsed fields
 */
public record ParsedDocument(Map<String, Object> fields) {
  public ParsedDocument {
    Objects.requireNonNull(fields, "fields is required");
    fields = Map.copyOf(fields);
  }
}
