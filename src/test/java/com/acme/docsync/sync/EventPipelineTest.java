package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionContractValidator;
import com.acme.docsync.model.IngestionOperation;
import com.acme.docsync.storage.TenantPathPolicy;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventPipelineTest {
  @Test
  void drainAndMarkProcessed_updatesProcessedState() {
    IngestionContractValidator validator = new IngestionContractValidator(
        new TenantPathPolicy());
    EventOrderingResolver resolver = new EventOrderingResolver();
    InMemoryProcessedEventStore store = new InMemoryProcessedEventStore();
    EventIngressQueue queue = new EventIngressQueue(4);
    EventPipeline pipeline = new EventPipeline(validator, resolver, store, queue);
    IngestionContract event = new IngestionContract(
        "Tenant1",
        "docs/a.json",
        IngestionOperation.EDIT,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
        "source-a",
        null);

    pipeline.accept(event);
    IngestionContract drained = pipeline.drainAndMarkProcessed();

    assertEquals("tenant1", drained.tenantId());
    assertTrue(store.wasRecentlyProcessed(
        drained.tenantId(), drained.path(), drained.timestamp()));
  }
}
