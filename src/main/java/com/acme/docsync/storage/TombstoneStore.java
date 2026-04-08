package com.acme.docsync.storage;

import com.acme.docsync.model.TombstoneRecord;

/**
 * Tombstone persistence contract.
 */
public interface TombstoneStore {
  void put(TombstoneRecord record);

  TombstoneRecord find(String tenantId, String canonicalPath);
}
