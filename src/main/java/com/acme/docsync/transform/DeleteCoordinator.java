package com.acme.docsync.transform;

import com.acme.docsync.model.DeletePolicy;
import com.acme.docsync.model.TombstoneRecord;
import com.acme.docsync.storage.CanonicalDocumentStore;
import com.acme.docsync.storage.IndexPublishStateStore;
import com.acme.docsync.storage.TombstoneStore;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates delete behavior across canonical and index state.
 */
public final class DeleteCoordinator {
  private final CanonicalDocumentStore canonicalStore;
  private final IndexPublishStateStore publishStateStore;
  private final TombstoneStore tombstoneStore;

  public DeleteCoordinator(
      CanonicalDocumentStore canonicalStore,
      IndexPublishStateStore publishStateStore,
      TombstoneStore tombstoneStore) {
    this.canonicalStore = Objects.requireNonNull(canonicalStore, "canonicalStore is required");
    this.publishStateStore = Objects.requireNonNull(
        publishStateStore, "publishStateStore is required");
    this.tombstoneStore = Objects.requireNonNull(tombstoneStore, "tombstoneStore is required");
  }

  public void applyDelete(
      String tenantId,
      String canonicalPath,
      DeletePolicy policy,
      Instant at) {
    tombstoneStore.put(new TombstoneRecord(tenantId, canonicalPath, policy, at));
    canonicalStore.delete(tenantId, canonicalPath, policy);
    publishStateStore.markDeleted(tenantId, canonicalPath, at);
  }
}
