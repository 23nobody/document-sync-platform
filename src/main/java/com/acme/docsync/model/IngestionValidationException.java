package com.acme.docsync.model;

/**
 * Raised when an ingestion contract violates required constraints.
 */
public final class IngestionValidationException extends RuntimeException {
  /**
   * Creates a validation exception.
   *
   * @param message actionable validation failure details
   */
  public IngestionValidationException(String message) {
    super(message);
  }
}
