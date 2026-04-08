package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionOperation;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventOrderingResolverTest {
  @Test
  void compare_ordersByTimestamp() {
    EventOrderingResolver resolver = new EventOrderingResolver();
    IngestionContract older = contract("tenant1", "a.json", IngestionOperation.ADD,
        Instant.parse("2026-01-01T00:00:00Z"));
    IngestionContract newer = contract("tenant1", "a.json", IngestionOperation.ADD,
        Instant.parse("2026-01-01T00:01:00Z"));

    int result = resolver.compare(older, newer);

    assertTrue(result < 0);
  }

  @Test
  void compare_usesDeterministicTieBreakers() {
    EventOrderingResolver resolver = new EventOrderingResolver();
    IngestionContract left = contract("tenant1", "b.json", IngestionOperation.DELETE,
        Instant.parse("2026-01-01T00:00:00Z"));
    IngestionContract right = contract("tenant1", "a.json", IngestionOperation.EDIT,
        Instant.parse("2026-01-01T00:00:00Z"));

    int result = resolver.compare(left, right);

    assertTrue(result < 0);
    assertEquals(-result, resolver.compare(right, left));
  }

  private IngestionContract contract(
      String tenantId, String path, IngestionOperation op, Instant timestamp) {
    return new IngestionContract(tenantId, path, op, null, timestamp, null, null);
  }
}
