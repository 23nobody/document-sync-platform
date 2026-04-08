package com.acme.docsync.transform;

import com.acme.docsync.model.CanonicalDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validates canonical output invariants.
 */
public final class CanonicalOutputValidator {
  /**
   * Validates canonical document.
   *
   * @param document canonical document
   * @return validation result
   */
  public CanonicalValidationResult validate(CanonicalDocument document) {
    Objects.requireNonNull(document, "document is required");
    List<String> violations = new ArrayList<>();
    if (document.tenantId().isBlank()) {
      violations.add("tenantId must not be blank");
    }
    if (document.canonicalPath().isBlank()) {
      violations.add("canonicalPath must not be blank");
    }
    if (document.fields().isEmpty()) {
      violations.add("fields must not be empty");
    }
    return violations.isEmpty()
        ? CanonicalValidationResult.success()
        : CanonicalValidationResult.failure(violations);
  }
}
