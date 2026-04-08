package com.acme.docsync.sync;

import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory counters for reconciliation runs.
 */
public final class ReconciliationMetrics {
  private final AtomicLong runs = new AtomicLong();
  private final AtomicLong created = new AtomicLong();
  private final AtomicLong modified = new AtomicLong();
  private final AtomicLong deleted = new AtomicLong();
  private final AtomicLong failedSources = new AtomicLong();

  /**
   * Records one completed run.
   *
   * @param runResult run result
   */
  public void recordRun(ReconciliationRunResult runResult) {
    runs.incrementAndGet();
    created.addAndGet(runResult.createdCount());
    modified.addAndGet(runResult.modifiedCount());
    deleted.addAndGet(runResult.deletedCount());
    failedSources.addAndGet(runResult.failedSources());
  }

  public long runs() {
    return runs.get();
  }

  public long created() {
    return created.get();
  }

  public long modified() {
    return modified.get();
  }

  public long deleted() {
    return deleted.get();
  }

  public long failedSources() {
    return failedSources.get();
  }
}
