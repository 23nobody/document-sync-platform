package com.acme.docsync.storage;

import com.acme.docsync.model.ProvenanceRecord;

/**
 * Provenance lookup store for source-canonical mappings.
 */
public interface ProvenanceStore {
  /**
   * Stores a provenance record.
   *
   * @param record provenance record
   */
  void put(ProvenanceRecord record);

  /**
   * Looks up provenance by source path.
   *
   * @param tenantId tenant identifier
   * @param sourcePath source path
   * @return record or null
   */
  ProvenanceRecord findBySource(String tenantId, String sourcePath);

  /**
   * Looks up provenance by canonical path.
   *
   * @param tenantId tenant identifier
   * @param canonicalPath canonical path
   * @return record or null
   */
  ProvenanceRecord findByCanonical(String tenantId, String canonicalPath);
}
