package com.acme.docsync.model;

/**
 * Delete policy for canonical and index state.
 */
public enum DeletePolicy {
  TOMBSTONE_ONLY,
  TOMBSTONE_AND_PHYSICAL_DELETE
}
