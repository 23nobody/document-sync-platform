package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeHealthServiceTest {
  @Test
  void snapshot_reportsQueueDepthAndSchedulerState() {
    EventIngressQueue queue = new EventIngressQueue(4);
    queue.offer(new IngestionContract(
        "tenant1",
        "docs/a.json",
        IngestionOperation.ADD,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
        "source-a",
        null));
    ReconciliationMetrics metrics = new ReconciliationMetrics();
    ReconciliationScheduler scheduler = new ReconciliationScheduler(
        new ReconciliationScanner(
            List.of(),
            new InMemorySourceSnapshotStore(),
            new DriftDetector(),
            new EventPipeline(
                new com.acme.docsync.model.IngestionContractValidator(
                    new com.acme.docsync.storage.TenantPathPolicy()),
                new EventOrderingResolver(),
                new InMemoryProcessedEventStore(),
                new EventIngressQueue(1))),
        metrics,
        Duration.ofMinutes(5));
    RuntimeHealthService service = new RuntimeHealthService(queue, scheduler, metrics);

    RuntimeHealthReport report = service.snapshot(false);

    assertEquals(1, report.queueDepth());
    assertFalse(report.schedulerRunning());
    assertFalse(report.watcherRunning());
  }
}
