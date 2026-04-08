package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.PublishStateRecord;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileIndexPublishStateStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void upsert_find_markDeleted_updatesState() {
    FileIndexPublishStateStore store = new FileIndexPublishStateStore(tempDir);
    PublishStateRecord record = new PublishStateRecord(
        "tenant1",
        "canonical/docs/a.json",
        "abc",
        Instant.parse("2026-01-01T00:00:00Z"),
        false);
    store.upsert(record);

    PublishStateRecord loaded = store.find("tenant1", "canonical/docs/a.json");
    assertNotNull(loaded);
    assertEquals("abc", loaded.checksum());

    store.markDeleted(
        "tenant1", "canonical/docs/a.json", Instant.parse("2026-01-01T00:01:00Z"));
    PublishStateRecord deleted = store.find("tenant1", "canonical/docs/a.json");
    assertTrue(deleted.deleted());
  }
}
