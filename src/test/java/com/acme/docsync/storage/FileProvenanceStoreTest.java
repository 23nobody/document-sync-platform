package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.docsync.model.ProvenanceRecord;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileProvenanceStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void put_andFindBySourceAndCanonical_roundTripsRecord() {
    FileProvenanceStore store = new FileProvenanceStore(tempDir);
    ProvenanceRecord record = new ProvenanceRecord(
        "tenant1",
        "docs/a.json",
        "tenant1/canonical/docs/a.json",
        "v1",
        Instant.parse("2026-01-01T00:00:00Z"));

    store.put(record);
    ProvenanceRecord bySource = store.findBySource("tenant1", "docs/a.json");
    ProvenanceRecord byCanonical = store.findByCanonical(
        "tenant1", "tenant1/canonical/docs/a.json");

    assertNotNull(bySource);
    assertNotNull(byCanonical);
    assertEquals("v1", bySource.sourceVersion());
    assertEquals("docs/a.json", byCanonical.sourcePath());
  }
}
