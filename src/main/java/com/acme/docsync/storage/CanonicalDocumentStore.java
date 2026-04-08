package com.acme.docsync.storage;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DeletePolicy;

/**
 * Canonical document persistence contract.
 */
public interface CanonicalDocumentStore {
  void put(CanonicalDocument document);

  CanonicalDocument get(String tenantId, String canonicalPath);

  void delete(String tenantId, String canonicalPath, DeletePolicy policy);
}
