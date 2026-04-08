package com.acme.docsync.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a file-level change event.
 */
public record DocumentEvent(
    String tenantId,
    String path,
    ChangeType changeType,
    String version,
    Instant eventTime
) {
  public DocumentEvent {
    Objects.requireNonNull(tenantId, "tenantId is required");
    Objects.requireNonNull(path, "path is required");
    Objects.requireNonNull(changeType, "changeType is required");
    Objects.requireNonNull(version, "version is required");
    Objects.requireNonNull(eventTime, "eventTime is required");
  }

  public enum ChangeType {
    ADD,
    EDIT,
    DELETE
  }
}
