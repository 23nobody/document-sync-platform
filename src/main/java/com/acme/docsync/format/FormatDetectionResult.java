package com.acme.docsync.format;

/**
 * Detection result with plugin confidence.
 *
 * @param supported whether plugin supports input
 * @param score confidence score in range [0, 1]
 */
public record FormatDetectionResult(boolean supported, double score) {
  public FormatDetectionResult {
    if (score < 0 || score > 1) {
      throw new IllegalArgumentException("score must be in range [0, 1]");
    }
  }

  /**
   * Creates a supported result.
   *
   * @param score confidence score
   * @return result
   */
  public static FormatDetectionResult supported(double score) {
    return new FormatDetectionResult(true, score);
  }

  /**
   * Creates an unsupported result.
   *
   * @return result
   */
  public static FormatDetectionResult unsupported() {
    return new FormatDetectionResult(false, 0);
  }
}
