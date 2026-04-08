package com.acme.docsync.transform;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.storage.FileCanonicalDocumentStore;
import com.acme.docsync.storage.FileIndexPublishStateStore;
import com.acme.docsync.storage.FileTombstoneStore;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PublishDeleteCoordinatorTest {
  @TempDir
  Path tempDir;

  @Test
  void publishThenDelete_keepsIndexAndTombstoneConsistent() {
    FileCanonicalDocumentStore canonicalStore = new FileCanonicalDocumentStore(
        tempDir.resolve("canonical"));
    FileIndexPublishStateStore indexStore = new FileIndexPublishStateStore(
        tempDir.resolve("index"));
    FileTombstoneStore tombstoneStore = new FileTombstoneStore(
        tempDir.resolve("tombstones"));
    PublishCoordinator publishCoordinator = new PublishCoordinator(canonicalStore, indexStore);
    DeleteCoordinator deleteCoordinator = new DeleteCoordinator(
        canonicalStore, indexStore, tombstoneStore);
    CanonicalDocument doc = new CanonicalDocument(
        "tenant1", "canonical/docs/a.json", Map.of("k", "v"));

    publishCoordinator.publish(doc, "abc", Instant.parse("2026-01-01T00:00:00Z"));
    deleteCoordinator.applyDelete(
        "tenant1",
        "canonical/docs/a.json",
        DeletePolicy.TOMBSTONE_AND_PHYSICAL_DELETE,
        Instant.parse("2026-01-01T00:01:00Z"));

    assertNotNull(tombstoneStore.find("tenant1", "canonical/docs/a.json"));
    assertTrue(indexStore.find("tenant1", "canonical/docs/a.json").deleted());
  }
}
