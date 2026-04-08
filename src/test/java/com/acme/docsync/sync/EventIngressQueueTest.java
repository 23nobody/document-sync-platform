package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionOperation;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventIngressQueueTest {
  @Test
  void offer_returnsFalseWhenQueueIsFull() {
    EventIngressQueue queue = new EventIngressQueue(1);
    assertTrue(queue.offer(event("a.json")));

    boolean accepted = queue.offer(event("b.json"));

    assertFalse(accepted);
    assertEquals(1, queue.size());
  }

  @Test
  void take_returnsSameEventThatWasOffered() {
    EventIngressQueue queue = new EventIngressQueue(2);
    IngestionContract expected = event("a.json");
    queue.offer(expected);

    IngestionContract actual = queue.take();

    assertEquals(expected, actual);
  }

  private IngestionContract event(String path) {
    return new IngestionContract(
        "tenant1",
        path,
        IngestionOperation.ADD,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
        "source-a",
        "req-1");
  }
}
