package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acme.docsync.model.IngestionContractValidator;
import com.acme.docsync.storage.TenantPathPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReconciliationScannerTest {
  @TempDir
  Path tempDir;

  @Test
  void runOnce_emitsCreatedThenConvergesOnRepeat() throws IOException {
    Path root = tempDir.resolve("source-a");
    Files.createDirectories(root.resolve("docs"));
    Files.writeString(root.resolve("docs/a.json"), "{\"a\":1}");

    EventPipeline pipeline = new EventPipeline(
        new IngestionContractValidator(new TenantPathPolicy()),
        new EventOrderingResolver(),
        new InMemoryProcessedEventStore(),
        new EventIngressQueue(32));
    ReconciliationScanner scanner = new ReconciliationScanner(
        List.of(new NioFileWatcherAdapter.WatchTarget("tenant1", "source-a", root)),
        new InMemorySourceSnapshotStore(),
        new DriftDetector(),
        pipeline);

    ReconciliationRunResult first = scanner.runOnce();
    ReconciliationRunResult second = scanner.runOnce();

    assertEquals(1, first.createdCount());
    assertEquals(0, first.modifiedCount());
    assertEquals(0, first.deletedCount());
    assertEquals(0, first.failedSources());
    assertEquals(0, second.createdCount());
    assertEquals(0, second.modifiedCount());
    assertEquals(0, second.deletedCount());
    assertEquals(0, second.failedSources());
  }

  @Test
  void runOnce_reportsFailedSourceForMissingRoot() {
    EventPipeline pipeline = new EventPipeline(
        new IngestionContractValidator(new TenantPathPolicy()),
        new EventOrderingResolver(),
        new InMemoryProcessedEventStore(),
        new EventIngressQueue(32));
    ReconciliationScanner scanner = new ReconciliationScanner(
        List.of(new NioFileWatcherAdapter.WatchTarget(
            "tenant1", "source-a", tempDir.resolve("missing"))),
        new InMemorySourceSnapshotStore(),
        new DriftDetector(),
        pipeline);

    ReconciliationRunResult result = scanner.runOnce();

    assertEquals(0, result.createdCount());
    assertEquals(0, result.modifiedCount());
    assertEquals(0, result.deletedCount());
    assertEquals(1, result.failedSources());
  }
}
