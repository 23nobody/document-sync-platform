package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionOperation;
import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Periodic scanner that reconciles source trees to event pipeline.
 */
public final class ReconciliationScanner {
  private final List<NioFileWatcherAdapter.WatchTarget> targets;
  private final SourceSnapshotStore snapshotStore;
  private final DriftDetector driftDetector;
  private final EventPipeline eventPipeline;

  /**
   * Creates a reconciliation scanner.
   *
   * @param targets tenant/source targets
   * @param snapshotStore source snapshot store
   * @param driftDetector drift detector
   * @param eventPipeline event pipeline
   */
  public ReconciliationScanner(
      List<NioFileWatcherAdapter.WatchTarget> targets,
      SourceSnapshotStore snapshotStore,
      DriftDetector driftDetector,
      EventPipeline eventPipeline) {
    this.targets = Objects.requireNonNull(targets, "targets is required");
    this.snapshotStore = Objects.requireNonNull(
        snapshotStore, "snapshotStore is required");
    this.driftDetector = Objects.requireNonNull(
        driftDetector, "driftDetector is required");
    this.eventPipeline = Objects.requireNonNull(
        eventPipeline, "eventPipeline is required");
  }

  /**
   * Runs one reconciliation pass over all configured targets.
   *
   * @return run summary
   */
  public ReconciliationRunResult runOnce() {
    int createdCount = 0;
    int modifiedCount = 0;
    int deletedCount = 0;
    int failedSources = 0;

    for (NioFileWatcherAdapter.WatchTarget target : targets) {
      try {
        SourceSnapshot current = capture(target);
        SourceSnapshot previous = snapshotStore.load(
            target.tenantId(), target.sourceId());
        DriftResult drift = driftDetector.detect(current, previous);

        createdCount += emit(target, drift.created(), IngestionOperation.ADD);
        modifiedCount += emit(target, drift.modified(), IngestionOperation.EDIT);
        deletedCount += emit(target, drift.deleted(), IngestionOperation.DELETE);
        snapshotStore.persist(current);
      } catch (RuntimeException e) {
        failedSources++;
      }
    }

    return new ReconciliationRunResult(
        createdCount, modifiedCount, deletedCount, failedSources);
  }

  private SourceSnapshot capture(NioFileWatcherAdapter.WatchTarget target) {
    Path root = target.rootPath();
    if (!Files.exists(root) || !Files.isDirectory(root)) {
      throw new IngestionValidationException("scan root is missing: " + root);
    }
    Map<String, Instant> filesByPath = new HashMap<>();
    try (Stream<Path> stream = Files.walk(root)) {
      stream.filter(Files::isRegularFile).forEach(path -> {
        Path relative = root.relativize(path);
        String normalized = relative.normalize().toString().replace('\\', '/');
        try {
          filesByPath.put(normalized, Files.getLastModifiedTime(path).toInstant());
        } catch (IOException e) {
          throw new IngestionValidationException(
              "failed to read last-modified for " + normalized);
        }
      });
    } catch (IOException e) {
      throw new IngestionValidationException("failed to walk source root: " + root);
    }
    return new SourceSnapshot(
        target.tenantId(), target.sourceId(), Instant.now(), filesByPath);
  }

  private int emit(
      NioFileWatcherAdapter.WatchTarget target,
      Iterable<String> paths,
      IngestionOperation operation) {
    int emitted = 0;
    for (String path : paths) {
      IngestionContract contract = new IngestionContract(
          target.tenantId(),
          path,
          operation,
          null,
          Instant.now(),
          target.sourceId(),
          "reconciliation");
      eventPipeline.accept(contract);
      emitted++;
    }
    return emitted;
  }
}
