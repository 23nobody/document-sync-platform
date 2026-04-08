package com.acme.docsync.sync;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Fixed-delay scheduler for reconciliation runs.
 */
public final class ReconciliationScheduler {
  private final ReconciliationScanner scanner;
  private final ReconciliationMetrics metrics;
  private final Duration interval;
  private ScheduledExecutorService executor;

  /**
   * Creates a reconciliation scheduler.
   *
   * @param scanner reconciliation scanner
   * @param metrics reconciliation metrics sink
   * @param interval run interval
   */
  public ReconciliationScheduler(
      ReconciliationScanner scanner,
      ReconciliationMetrics metrics,
      Duration interval) {
    this.scanner = Objects.requireNonNull(scanner, "scanner is required");
    this.metrics = Objects.requireNonNull(metrics, "metrics is required");
    this.interval = Objects.requireNonNull(interval, "interval is required");
  }

  /**
   * Starts periodic reconciliation.
   */
  public synchronized void start() {
    if (executor != null) {
      return;
    }
    executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleWithFixedDelay(() -> {
      ReconciliationRunResult result = scanner.runOnce();
      metrics.recordRun(result);
    }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Stops periodic reconciliation.
   */
  public synchronized void stop() {
    if (executor == null) {
      return;
    }
    executor.shutdownNow();
    executor = null;
  }

  /**
   * Indicates whether scheduler is running.
   *
   * @return true when running
   */
  public synchronized boolean isRunning() {
    return executor != null && !executor.isShutdown();
  }
}
