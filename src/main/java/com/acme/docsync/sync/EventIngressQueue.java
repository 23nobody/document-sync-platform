package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionValidationException;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Bounded ingress queue for ingestion contracts.
 */
public final class EventIngressQueue {
  private final BlockingQueue<IngestionContract> queue;

  /**
   * Creates a bounded event ingress queue.
   *
   * @param capacity queue capacity
   */
  public EventIngressQueue(int capacity) {
    if (capacity <= 0) {
      throw new IngestionValidationException("capacity must be greater than zero");
    }
    this.queue = new ArrayBlockingQueue<>(capacity);
  }

  /**
   * Offers an event to the queue.
   *
   * @param event validated ingestion event
   * @return true when enqueued
   */
  public boolean offer(IngestionContract event) {
    Objects.requireNonNull(event, "event is required");
    return queue.offer(event);
  }

  /**
   * Takes the next event from the queue.
   *
   * @return next event
   */
  public IngestionContract take() {
    try {
      return queue.take();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IngestionValidationException(
          "interrupted while waiting for ingress event");
    }
  }

  /**
   * Returns current queue size.
   *
   * @return queue size
   */
  public int size() {
    return queue.size();
  }
}
