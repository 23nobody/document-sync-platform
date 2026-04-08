package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryProcessedEventStore implements ProcessedEventStore {
  private final Map<String, Instant> store = new ConcurrentHashMap<>();

  @Override
  public void markProcessed(IngestionContract event) {
    store.put(key(event.tenantId(), event.path()), event.timestamp());
  }

  @Override
  public boolean wasRecentlyProcessed(String tenantId, String path, Instant timestamp) {
    Instant current = store.get(key(tenantId, path));
    return timestamp != null && timestamp.equals(current);
  }

  @Override
  public Instant lastProcessedAt(String tenantId, String path) {
    if (tenantId == null || path == null) {
      return null;
    }
    return store.get(key(tenantId, path));
  }

  private String key(String tenantId, String path) {
    return tenantId + "|" + path;
  }
}
