package com.acme.docsync.storage;

import java.util.List;
import java.util.Objects;

/**
 * Consistency check report across canonical/index/tombstone stores.
 *
 * @param consistent whether stores are consistent
 * @param issues issue list
 */
public record ConsistencyReport(boolean consistent, List<String> issues) {
  public ConsistencyReport {
    Objects.requireNonNull(issues, "issues is required");
    issues = List.copyOf(issues);
  }
}
