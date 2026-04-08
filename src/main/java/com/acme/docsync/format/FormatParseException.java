package com.acme.docsync.format;

/**
 * Parsing failure for source documents.
 */
public final class FormatParseException extends Exception {
  /**
   * Creates a parse exception.
   *
   * @param message error message
   * @param cause source cause
   */
  public FormatParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
