package com.acme.docsync.model;

import java.util.Objects;

/**
 * Stable format fingerprint descriptor.
 *
 * @param value fingerprint value
 * @param algorithm hashing algorithm identifier
 */
public record FormatFingerprint(String value, String algorithm) {
  public FormatFingerprint {
    Objects.requireNonNull(value, "value is required");
    Objects.requireNonNull(algorithm, "algorithm is required");
  }
}
