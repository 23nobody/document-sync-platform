package com.acme.docsync.sync;

import java.util.Objects;

/**
 * Produces runtime health snapshots.
 */
public final class RuntimeHealthService {
  private final EventIngressQueue ingressQueue;
  private final ReconciliationScheduler reconciliationScheduler;
  private final ReconciliationMetrics reconciliationMetrics;

  public RuntimeHealthService(
      EventIngressQueue ingressQueue,
      ReconciliationScheduler reconciliationScheduler,
      ReconciliationMetrics reconciliationMetrics) {
    this.ingressQueue = Objects.requireNonNull(ingressQueue, "ingressQueue is required");
    this.reconciliationScheduler = Objects.requireNonNull(
        reconciliationScheduler, "reconciliationScheduler is required");
    this.reconciliationMetrics = Objects.requireNonNull(
        reconciliationMetrics, "reconciliationMetrics is required");
  }

  /**
   * Creates a health report.
   *
   * @param watcherRunning watcher running status
   * @return runtime health report
   */
  public RuntimeHealthReport snapshot(boolean watcherRunning) {
    return new RuntimeHealthReport(
        watcherRunning,
        reconciliationScheduler.isRunning(),
        ingressQueue.size(),
        reconciliationMetrics.failedSources());
  }
}
