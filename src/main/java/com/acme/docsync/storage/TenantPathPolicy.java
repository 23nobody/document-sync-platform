package com.acme.docsync.storage;

import com.acme.docsync.model.IngestionValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tenant ID and relative path safety policy.
 */
public final class TenantPathPolicy {
  private static final Pattern TENANT_PATTERN =
      Pattern.compile("^[a-z0-9][a-z0-9_-]{1,62}$");

  /**
   * Normalizes and validates tenant ID.
   *
   * @param rawTenantId tenant input
   * @return normalized tenant ID
   */
  public String normalizeTenantId(String rawTenantId) {
    if (rawTenantId == null) {
      throw new IngestionValidationException("tenantId is required");
    }
    String normalized = rawTenantId.trim().toLowerCase(Locale.ROOT);
    if (!TENANT_PATTERN.matcher(normalized).matches()) {
      throw new IngestionValidationException(
          "tenantId must match ^[a-z0-9][a-z0-9_-]{1,62}$");
    }
    return normalized;
  }

  /**
   * Validates that the path is relative and non-traversing.
   *
   * @param rawPath relative path input
   */
  public void assertRelativePathSafe(String rawPath) {
    if (rawPath == null || rawPath.trim().isEmpty()) {
      throw new IngestionValidationException("path is required");
    }
    String path = rawPath.trim();
    if (path.startsWith("/") || path.startsWith("\\") || path.contains(":")) {
      throw new IngestionValidationException("path must be relative");
    }
    if (path.contains("..")) {
      throw new IngestionValidationException("path traversal is not allowed");
    }
  }
}
