package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.model.PublishStateRecord;
import com.acme.docsync.model.TombstoneRecord;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConsistencyCheckerTest {
  @TempDir
  Path tempDir;

  @Test
  void run_reportsIssueWhenIndexMissingForCanonical() {
    FileCanonicalDocumentStore canonicalStore = new FileCanonicalDocumentStore(
        tempDir.resolve("canonical"));
    FileIndexPublishStateStore indexStore = new FileIndexPublishStateStore(
        tempDir.resolve("index"));
    FileTombstoneStore tombstoneStore = new FileTombstoneStore(tempDir.resolve("tomb"));
    ConsistencyChecker checker = new ConsistencyChecker(
        canonicalStore, indexStore, tombstoneStore);

    canonicalStore.put(new CanonicalDocument("tenant1", "canonical/a.json", Map.of("a", 1)));

    ConsistencyReport report = checker.run("tenant1", "canonical/a.json");
    assertFalse(report.consistent());
  }

  @Test
  void run_isConsistentForDeletedStateWithTombstone() {
    FileCanonicalDocumentStore canonicalStore = new FileCanonicalDocumentStore(
        tempDir.resolve("canonical"));
    FileIndexPublishStateStore indexStore = new FileIndexPublishStateStore(
        tempDir.resolve("index"));
    FileTombstoneStore tombstoneStore = new FileTombstoneStore(tempDir.resolve("tomb"));
    ConsistencyChecker checker = new ConsistencyChecker(
        canonicalStore, indexStore, tombstoneStore);

    indexStore.upsert(new PublishStateRecord(
        "tenant1", "canonical/a.json", "abc", Instant.parse("2026-01-01T00:00:00Z"), true));
    tombstoneStore.put(new TombstoneRecord(
        "tenant1",
        "canonical/a.json",
        DeletePolicy.TOMBSTONE_ONLY,
        Instant.parse("2026-01-01T00:00:00Z")));

    ConsistencyReport report = checker.run("tenant1", "canonical/a.json");
    assertTrue(report.consistent());
  }
}
