package com.acme.docsync.transform;

/**
 * Transformation failure for canonical conversion.
 */
public final class TransformException extends Exception {
  /**
   * Creates a transformation exception.
   *
   * @param message error message
   * @param cause source cause
   */
  public TransformException(String message, Throwable cause) {
    super(message, cause);
  }
}
