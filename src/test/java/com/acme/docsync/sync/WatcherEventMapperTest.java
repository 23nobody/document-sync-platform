package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionContractValidator;
import com.acme.docsync.model.IngestionOperation;
import com.acme.docsync.model.IngestionValidationException;
import com.acme.docsync.storage.TenantPathPolicy;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WatcherEventMapperTest {
  @Test
  void toIngestionEvent_mapsCreateToAdd() {
    WatcherEventMapper mapper = new WatcherEventMapper(
        new IngestionContractValidator(new TenantPathPolicy()));
    RawWatcherEvent raw = new RawWatcherEvent(
        "Tenant1",
        "source-a",
        Path.of("docs/file.json"),
        RawWatcherEvent.Kind.CREATE,
        Instant.parse("2026-01-01T00:00:00Z"));

    IngestionContract mapped = mapper.toIngestionEvent(raw, Instant.now());

    assertEquals("tenant1", mapped.tenantId());
    assertEquals("docs/file.json", mapped.path());
    assertEquals(IngestionOperation.ADD, mapped.operation());
  }

  @Test
  void toIngestionEvent_rejectsMissingRelativePath() {
    WatcherEventMapper mapper = new WatcherEventMapper(
        new IngestionContractValidator(new TenantPathPolicy()));
    RawWatcherEvent raw = new RawWatcherEvent(
        "tenant1", "source-a", null, RawWatcherEvent.Kind.DELETE, Instant.now());

    IngestionValidationException exception = assertThrows(
        IngestionValidationException.class,
        () -> mapper.toIngestionEvent(raw, Instant.now()));

    assertEquals("relativePath is required", exception.getMessage());
  }
}
