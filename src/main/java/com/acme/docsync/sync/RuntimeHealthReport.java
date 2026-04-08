package com.acme.docsync.sync;

import java.util.Objects;

/**
 * Runtime health snapshot for sync components.
 *
 * @param watcherRunning watcher running state
 * @param schedulerRunning reconciliation scheduler state
 * @param queueDepth current ingress queue depth
 * @param failedSources cumulative failed reconciliation sources
 */
public record RuntimeHealthReport(
    boolean watcherRunning,
    boolean schedulerRunning,
    int queueDepth,
    long failedSources
) {
  public RuntimeHealthReport {
    if (queueDepth < 0) {
      throw new IllegalArgumentException("queueDepth must be non-negative");
    }
    if (failedSources < 0) {
      throw new IllegalArgumentException("failedSources must be non-negative");
    }
  }
}
