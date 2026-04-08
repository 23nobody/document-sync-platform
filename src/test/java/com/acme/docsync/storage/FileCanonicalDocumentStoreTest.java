package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DeletePolicy;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileCanonicalDocumentStoreTest {
  @TempDir
  Path tempDir;

  @Test
  void put_get_delete_roundTripsWithPhysicalDelete() {
    FileCanonicalDocumentStore store = new FileCanonicalDocumentStore(tempDir);
    CanonicalDocument doc = new CanonicalDocument(
        "tenant1", "canonical/docs/a.json", Map.of("a", 1));

    store.put(doc);
    assertNotNull(store.get("tenant1", "canonical/docs/a.json"));

    store.delete("tenant1", "canonical/docs/a.json",
        DeletePolicy.TOMBSTONE_AND_PHYSICAL_DELETE);
    assertNull(store.get("tenant1", "canonical/docs/a.json"));
  }
}
