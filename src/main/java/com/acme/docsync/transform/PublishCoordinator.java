package com.acme.docsync.transform;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.PublishStateRecord;
import com.acme.docsync.storage.CanonicalDocumentStore;
import com.acme.docsync.storage.IndexPublishStateStore;
import java.time.Instant;
import java.util.Objects;

/**
 * Coordinates canonical persistence and publish-state updates.
 */
public final class PublishCoordinator {
  private final CanonicalDocumentStore canonicalStore;
  private final IndexPublishStateStore publishStateStore;

  public PublishCoordinator(
      CanonicalDocumentStore canonicalStore,
      IndexPublishStateStore publishStateStore) {
    this.canonicalStore = Objects.requireNonNull(canonicalStore, "canonicalStore is required");
    this.publishStateStore = Objects.requireNonNull(
        publishStateStore, "publishStateStore is required");
  }

  public void publish(CanonicalDocument doc, String checksum, Instant at) {
    canonicalStore.put(doc);
    publishStateStore.upsert(new PublishStateRecord(
        doc.tenantId(), doc.canonicalPath(), checksum, at, false));
  }
}
