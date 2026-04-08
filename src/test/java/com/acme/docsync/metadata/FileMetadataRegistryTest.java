package com.acme.docsync.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.acme.docsync.model.DirectoryMetadataProfile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileMetadataRegistryTest {
  @TempDir
  Path tempDir;

  @Test
  void upsertProfile_incrementsVersionOnlyWhenFingerprintChanges() {
    FileMetadataRegistry registry = new FileMetadataRegistry(tempDir);
    FormatProfiler profiler = new FormatProfiler(new FingerprintCalculator());
    DirectoryMetadataProfile first = profiler.profile(
        "tenant1",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of("mode", "strict"),
        List.of("a", "b"),
        Instant.parse("2026-01-01T00:00:00Z"));
    DirectoryMetadataProfile sameFingerprint = profiler.profile(
        "tenant1",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of("mode", "strict"),
        List.of("b", "a"),
        Instant.parse("2026-01-01T01:00:00Z"));
    DirectoryMetadataProfile changedFingerprint = profiler.profile(
        "tenant1",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of("mode", "strict"),
        List.of("a", "c"),
        Instant.parse("2026-01-01T02:00:00Z"));

    registry.upsertProfile(first);
    registry.upsertProfile(sameFingerprint);
    registry.upsertProfile(changedFingerprint);

    DirectoryMetadataProfile latest = registry.getLatest("tenant1", "docs");
    assertNotNull(latest);
    assertEquals(2, latest.version());
    assertEquals(2, registry.history("tenant1", "docs").size());
  }

  @Test
  void getLatest_andHistory_areTenantScoped() {
    FileMetadataRegistry registry = new FileMetadataRegistry(tempDir);
    FormatProfiler profiler = new FormatProfiler(new FingerprintCalculator());
    registry.upsertProfile(profiler.profile(
        "tenant1",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of(),
        List.of("a"),
        Instant.parse("2026-01-01T00:00:00Z")));
    registry.upsertProfile(profiler.profile(
        "tenant2",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of(),
        List.of("b"),
        Instant.parse("2026-01-01T00:00:00Z")));

    DirectoryMetadataProfile tenantOne = registry.getLatest("tenant1", "docs");
    DirectoryMetadataProfile tenantTwo = registry.getLatest("tenant2", "docs");

    assertEquals("tenant1", tenantOne.tenantId());
    assertEquals("tenant2", tenantTwo.tenantId());
    assertEquals(1, registry.history("tenant1", "docs").size());
    assertEquals(1, registry.history("tenant2", "docs").size());
  }
}
