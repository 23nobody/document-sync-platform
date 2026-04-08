package com.acme.docsync.sync;

/**
 * Stores and retrieves source snapshots for reconciliation.
 */
public interface SourceSnapshotStore {
  /**
   * Loads the previous snapshot for the tenant/source key.
   *
   * @param tenantId tenant identifier
   * @param sourceId source identifier
   * @return snapshot or null when not found
   */
  SourceSnapshot load(String tenantId, String sourceId);

  /**
   * Persists a snapshot for the tenant/source key.
   *
   * @param snapshot snapshot to persist
   */
  void persist(SourceSnapshot snapshot);
}
