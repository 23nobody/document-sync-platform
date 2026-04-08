package com.acme.docsync.sync;

/**
 * Filesystem watcher abstraction for near-real-time event ingestion.
 */
public interface FileWatcherAdapter {
  /**
   * Starts watcher event ingestion.
   */
  void start();

  /**
   * Stops watcher event ingestion.
   */
  void stop();

  /**
   * Indicates whether watcher is running.
   *
   * @return true when running
   */
  boolean isRunning();
}
