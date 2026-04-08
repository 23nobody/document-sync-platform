package com.acme.docsync.sync;

import java.nio.file.Path;
import java.time.Instant;

/**
 * Raw filesystem watcher event before ingestion mapping.
 *
 * @param tenantId tenant identifier
 * @param sourceId source identifier
 * @param relativePath path relative to source root
 * @param kind watcher event kind
 * @param occurredAt event timestamp
 */
public record RawWatcherEvent(
    String tenantId,
    String sourceId,
    Path relativePath,
    Kind kind,
    Instant occurredAt
) {
  /**
   * Supported watcher event kinds.
   */
  public enum Kind {
    CREATE,
    MODIFY,
    DELETE
  }
}
