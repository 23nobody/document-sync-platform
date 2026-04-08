package com.acme.docsync.storage;

import com.acme.docsync.model.PublishStateRecord;
import java.time.Instant;

/**
 * Local index publish state persistence contract.
 */
public interface IndexPublishStateStore {
  void upsert(PublishStateRecord record);

  PublishStateRecord find(String tenantId, String canonicalPath);

  void markDeleted(String tenantId, String canonicalPath, Instant at);
}
