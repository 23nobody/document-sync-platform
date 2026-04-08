package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionContractValidator;
import java.util.Objects;

/**
 * Event pipeline for validated ingress and processed-state tracking.
 */
public final class EventPipeline {
  private final IngestionContractValidator validator;
  private final EventOrderingResolver orderingResolver;
  private final ProcessedEventStore processedEventStore;
  private final EventIngressQueue ingressQueue;
  private IngestionContract lastAccepted;

  /**
   * Creates an event pipeline.
   *
   * @param validator contract validator
   * @param orderingResolver ordering resolver
   * @param processedEventStore processed-state store
   * @param ingressQueue ingress queue
   */
  public EventPipeline(
      IngestionContractValidator validator,
      EventOrderingResolver orderingResolver,
      ProcessedEventStore processedEventStore,
      EventIngressQueue ingressQueue) {
    this.validator = Objects.requireNonNull(validator, "validator is required");
    this.orderingResolver = Objects.requireNonNull(
        orderingResolver, "orderingResolver is required");
    this.processedEventStore = Objects.requireNonNull(
        processedEventStore, "processedEventStore is required");
    this.ingressQueue = Objects.requireNonNull(
        ingressQueue, "ingressQueue is required");
  }

  /**
   * Accepts and enqueues an event.
   *
   * @param event candidate event
   */
  public synchronized void accept(IngestionContract event) {
    IngestionContract normalized = validator.normalizeAndValidate(event);
    if (lastAccepted != null && orderingResolver.compare(lastAccepted, normalized) > 0) {
      lastAccepted = normalized;
    } else if (lastAccepted == null) {
      lastAccepted = normalized;
    } else {
      lastAccepted = normalized;
    }
    if (!ingressQueue.offer(normalized)) {
      throw new IllegalStateException("ingress queue is full");
    }
  }

  /**
   * Drains a single event and marks it processed.
   *
   * @return processed event
   */
  public IngestionContract drainAndMarkProcessed() {
    IngestionContract next = ingressQueue.take();
    processedEventStore.markProcessed(next);
    return next;
  }
}
