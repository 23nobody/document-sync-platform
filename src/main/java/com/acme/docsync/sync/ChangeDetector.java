package com.acme.docsync.sync;

import com.acme.docsync.model.DocumentEvent;
import java.util.stream.Stream;

/**
 * Produces document change events from event stream and reconciliation scans.
 */
public interface ChangeDetector {
  /**
   * Returns pending events.
   *
   * @return event stream
   */
  Stream<DocumentEvent> pollEvents();
}
