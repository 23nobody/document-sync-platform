package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionValidationException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NIO watch-service adapter for source directory updates.
 */
public final class NioFileWatcherAdapter implements FileWatcherAdapter {
  private final WatchService watchService;
  private final WatcherEventMapper mapper;
  private final EventPipeline pipeline;
  private final Map<WatchKey, WatchTarget> targetsByKey;
  private volatile boolean running;
  private Thread worker;

  /**
   * Creates a watcher adapter for source directories.
   *
   * @param targets tenant/source roots to watch
   * @param mapper raw event mapper
   * @param pipeline event pipeline
   */
  public NioFileWatcherAdapter(
      List<WatchTarget> targets, WatcherEventMapper mapper, EventPipeline pipeline) {
    try {
      this.watchService = FileSystems.getDefault().newWatchService();
    } catch (IOException e) {
      throw new IngestionValidationException("failed to create watch service");
    }
    this.mapper = Objects.requireNonNull(mapper, "mapper is required");
    this.pipeline = Objects.requireNonNull(pipeline, "pipeline is required");
    this.targetsByKey = new ConcurrentHashMap<>();
    registerTargets(targets);
  }

  @Override
  public synchronized void start() {
    if (running) {
      return;
    }
    running = true;
    worker = new Thread(this::runLoop, "docsync-file-watcher");
    worker.start();
  }

  @Override
  public synchronized void stop() {
    running = false;
    if (worker != null) {
      worker.interrupt();
    }
    try {
      watchService.close();
    } catch (IOException e) {
      throw new IngestionValidationException("failed to close watch service");
    }
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  private void registerTargets(List<WatchTarget> targets) {
    if (targets == null || targets.isEmpty()) {
      throw new IngestionValidationException("at least one watch target is required");
    }
    for (WatchTarget target : targets) {
      if (target == null || target.rootPath() == null) {
        throw new IngestionValidationException("watch target rootPath is required");
      }
      if (!Files.exists(target.rootPath()) || !Files.isDirectory(target.rootPath())) {
        throw new IngestionValidationException(
            "watch target root does not exist: " + target.rootPath());
      }
      try {
        WatchKey key = target.rootPath().register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE);
        targetsByKey.put(key, target);
      } catch (IOException e) {
        throw new IngestionValidationException(
            "failed to register watch target: " + target.rootPath());
      }
    }
  }

  private void runLoop() {
    while (running) {
      WatchKey key = takeKey();
      if (key == null) {
        continue;
      }
      WatchTarget target = targetsByKey.get(key);
      if (target == null) {
        key.reset();
        continue;
      }

      for (WatchEvent<?> event : key.pollEvents()) {
        if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
          continue;
        }
        @SuppressWarnings("unchecked")
        WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
        RawWatcherEvent rawEvent = new RawWatcherEvent(
            target.tenantId(),
            target.sourceId(),
            pathEvent.context(),
            mapKind(event.kind()),
            Instant.now());
        pipeline.accept(mapper.toIngestionEvent(rawEvent, Instant.now()));
      }
      key.reset();
    }
  }

  private WatchKey takeKey() {
    try {
      return watchService.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private RawWatcherEvent.Kind mapKind(WatchEvent.Kind<?> kind) {
    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
      return RawWatcherEvent.Kind.CREATE;
    }
    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
      return RawWatcherEvent.Kind.MODIFY;
    }
    if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
      return RawWatcherEvent.Kind.DELETE;
    }
    throw new IngestionValidationException("unsupported watcher event kind");
  }

  /**
   * Immutable watch target registration.
   *
   * @param tenantId tenant ID
   * @param sourceId source ID
   * @param rootPath watched source root
   */
  public record WatchTarget(String tenantId, String sourceId, Path rootPath) {
  }
}
