package com.acme.docsync.storage;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.PublishStateRecord;
import com.acme.docsync.model.TombstoneRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Verifies parity between canonical, index, and tombstone states.
 */
public final class ConsistencyChecker {
  private final CanonicalDocumentStore canonicalStore;
  private final IndexPublishStateStore indexStore;
  private final TombstoneStore tombstoneStore;

  public ConsistencyChecker(
      CanonicalDocumentStore canonicalStore,
      IndexPublishStateStore indexStore,
      TombstoneStore tombstoneStore) {
    this.canonicalStore = Objects.requireNonNull(canonicalStore, "canonicalStore is required");
    this.indexStore = Objects.requireNonNull(indexStore, "indexStore is required");
    this.tombstoneStore = Objects.requireNonNull(tombstoneStore, "tombstoneStore is required");
  }

  /**
   * Checks consistency for a canonical key.
   *
   * @param tenantId tenant identifier
   * @param canonicalPath canonical path
   * @return consistency report
   */
  public ConsistencyReport run(String tenantId, String canonicalPath) {
    List<String> issues = new ArrayList<>();
    CanonicalDocument canonical = canonicalStore.get(tenantId, canonicalPath);
    PublishStateRecord index = indexStore.find(tenantId, canonicalPath);
    TombstoneRecord tombstone = tombstoneStore.find(tenantId, canonicalPath);

    if (canonical != null && index == null) {
      issues.add("index state missing for existing canonical document");
    }
    if (index != null && index.deleted() && tombstone == null) {
      issues.add("tombstone missing for deleted index state");
    }
    if (tombstone != null && canonical != null && tombstone.policy().name().contains("PHYSICAL")) {
      issues.add("canonical document exists despite physical delete tombstone");
    }
    return new ConsistencyReport(issues.isEmpty(), issues);
  }
}
