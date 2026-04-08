package com.acme.docsync.transform;

import java.util.List;
import java.util.Objects;

/**
 * Result of canonical output validation.
 *
 * @param valid whether output is valid
 * @param violations violation messages
 */
public record CanonicalValidationResult(boolean valid, List<String> violations) {
  public CanonicalValidationResult {
    Objects.requireNonNull(violations, "violations is required");
    violations = List.copyOf(violations);
  }

  /**
   * Creates a success result.
   *
   * @return result
   */
  public static CanonicalValidationResult success() {
    return new CanonicalValidationResult(true, List.of());
  }

  /**
   * Creates a failure result.
   *
   * @param violations violation list
   * @return result
   */
  public static CanonicalValidationResult failure(List<String> violations) {
    return new CanonicalValidationResult(false, violations);
  }
}
