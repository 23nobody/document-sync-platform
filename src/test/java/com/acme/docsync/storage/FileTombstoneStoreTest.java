package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.model.TombstoneRecord;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTombstoneStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void put_find_roundTripsRecord() {
    FileTombstoneStore store = new FileTombstoneStore(tempDir);
    TombstoneRecord record = new TombstoneRecord(
        "tenant1",
        "canonical/docs/a.json",
        DeletePolicy.TOMBSTONE_ONLY,
        Instant.parse("2026-01-01T00:00:00Z"));
    store.put(record);

    TombstoneRecord loaded = store.find("tenant1", "canonical/docs/a.json");
    assertNotNull(loaded);
    assertEquals(DeletePolicy.TOMBSTONE_ONLY, loaded.policy());
  }
}
