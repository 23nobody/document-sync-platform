package com.acme.docsync.storage;

import java.util.stream.Stream;

/**
 * Tenant-isolated storage abstraction.
 */
public interface TenantStorage {
  /**
   * Lists documents for a tenant and optional prefix.
   *
   * @param tenantId tenant identifier
   * @param prefix optional path prefix, may be null
   * @return stream of object keys
   */
  Stream<String> list(String tenantId, String prefix);
}
