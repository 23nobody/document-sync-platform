package com.acme.docsync.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DriftDetectorTest {
  @Test
  void detect_classifiesCreatedModifiedDeletedAndUnchanged() {
    DriftDetector detector = new DriftDetector();
    SourceSnapshot previous = new SourceSnapshot(
        "tenant1",
        "source-a",
        Instant.parse("2026-01-01T00:00:00Z"),
        Map.of(
            "same.json", Instant.parse("2026-01-01T00:00:00Z"),
            "old.json", Instant.parse("2026-01-01T00:00:00Z"),
            "gone.json", Instant.parse("2026-01-01T00:00:00Z")));
    SourceSnapshot current = new SourceSnapshot(
        "tenant1",
        "source-a",
        Instant.parse("2026-01-01T00:01:00Z"),
        Map.of(
            "same.json", Instant.parse("2026-01-01T00:00:00Z"),
            "old.json", Instant.parse("2026-01-01T00:01:00Z"),
            "new.json", Instant.parse("2026-01-01T00:01:00Z")));

    DriftResult result = detector.detect(current, previous);

    assertEquals(1, result.created().size());
    assertEquals(1, result.modified().size());
    assertEquals(1, result.deleted().size());
    assertEquals(1, result.unchanged().size());
    assertTrue(result.created().contains("new.json"));
  }
}
