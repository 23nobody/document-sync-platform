package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import java.time.Instant;

/**
 * Stores minimal processed event state for restart continuity.
 */
public interface ProcessedEventStore {
  /**
   * Marks an event as processed.
   *
   * @param event processed event
   */
  void markProcessed(IngestionContract event);

  /**
   * Checks whether an event at timestamp was already recorded.
   *
   * @param tenantId tenant identifier
   * @param path relative path
   * @param timestamp event timestamp
   * @return true when matching timestamp exists
   */
  boolean wasRecentlyProcessed(String tenantId, String path, Instant timestamp);

  /**
   * Returns the last processed timestamp for a path.
   *
   * @param tenantId tenant identifier
   * @param path relative path
   * @return last processed timestamp or null when missing
   */
  Instant lastProcessedAt(String tenantId, String path);
}
