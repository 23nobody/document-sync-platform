package com.acme.docsync.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acme.docsync.model.IngestionValidationException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TenantPathResolverTest {
  @Test
  void resolveSourcePath_buildsTenantScopedPath() {
    TenantPathResolver resolver = new TenantPathResolver(
        Path.of("/tmp/docsync"), new TenantPathPolicy());

    Path resolved = resolver.resolveSourcePath("Tenant1", "source-a", "dir/a.json");

    assertTrue(resolved.toString().contains("tenants/tenant1/sources/source-a"));
    assertTrue(resolved.endsWith(Path.of("dir/a.json")));
  }

  @Test
  void resolveSourcePath_rejectsTraversal() {
    TenantPathResolver resolver = new TenantPathResolver(
        Path.of("/tmp/docsync"), new TenantPathPolicy());

    IngestionValidationException exception = assertThrows(
        IngestionValidationException.class,
        () -> resolver.resolveSourcePath("tenant1", "source-a", "../etc/passwd"));

    assertEquals("path traversal is not allowed", exception.getMessage());
  }
}
