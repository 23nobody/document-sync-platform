package com.acme.docsync.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemorySourceSnapshotStore implements SourceSnapshotStore {
  private final Map<String, SourceSnapshot> store = new ConcurrentHashMap<>();

  @Override
  public SourceSnapshot load(String tenantId, String sourceId) {
    return store.get(key(tenantId, sourceId));
  }

  @Override
  public void persist(SourceSnapshot snapshot) {
    store.put(key(snapshot.tenantId(), snapshot.sourceId()), snapshot);
  }

  private String key(String tenantId, String sourceId) {
    return tenantId + "|" + sourceId;
  }
}
