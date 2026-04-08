package com.acme.docsync.storage;

import com.acme.docsync.model.IngestionValidationException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolves tenant-scoped filesystem paths safely.
 */
public final class TenantPathResolver {
  private final Path baseDirectory;
  private final TenantPathPolicy tenantPathPolicy;

  /**
   * Creates a tenant path resolver.
   *
   * @param baseDirectory storage root
   * @param tenantPathPolicy tenant/path safety policy
   */
  public TenantPathResolver(Path baseDirectory, TenantPathPolicy tenantPathPolicy) {
    this.baseDirectory = Objects.requireNonNull(
        baseDirectory, "baseDirectory is required").toAbsolutePath().normalize();
    this.tenantPathPolicy = Objects.requireNonNull(
        tenantPathPolicy, "tenantPathPolicy is required");
  }

  /**
   * Resolves a source path within the given tenant root.
   *
   * @param tenantId tenant identifier
   * @param sourceId source identifier
   * @param relativePath tenant-relative path
   * @return resolved absolute path
   */
  public Path resolveSourcePath(String tenantId, String sourceId, String relativePath) {
    String normalizedTenantId = tenantPathPolicy.normalizeTenantId(tenantId);
    tenantPathPolicy.assertRelativePathSafe(relativePath);
    tenantPathPolicy.assertRelativePathSafe(sourceId);

    Path tenantRoot = baseDirectory.resolve(
        Path.of("tenants", normalizedTenantId, "sources", sourceId)).normalize();
    Path resolved = tenantRoot.resolve(relativePath).normalize();
    assertWithinRoot(tenantRoot, resolved, "source");
    return resolved;
  }

  /**
   * Resolves the canonical root directory for a tenant.
   *
   * @param tenantId tenant identifier
   * @return canonical root path
   */
  public Path resolveCanonicalRoot(String tenantId) {
    String normalizedTenantId = tenantPathPolicy.normalizeTenantId(tenantId);
    Path tenantRoot = baseDirectory.resolve(
        Path.of("tenants", normalizedTenantId, "canonical")).normalize();
    assertWithinRoot(baseDirectory, tenantRoot, "canonical-root");
    return tenantRoot;
  }

  private void assertWithinRoot(Path root, Path candidate, String context) {
    if (!candidate.startsWith(root)) {
      throw new IngestionValidationException(
          "resolved " + context + " path escapes tenant root");
    }
  }
}
