package com.acme.docsync.metadata;

import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.FormatFingerprint;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds metadata profiles and format fingerprints.
 */
public final class FormatProfiler {
  private final FingerprintCalculator fingerprintCalculator;

  /**
   * Creates a format profiler.
   *
   * @param fingerprintCalculator fingerprint calculator
   */
  public FormatProfiler(FingerprintCalculator fingerprintCalculator) {
    this.fingerprintCalculator = Objects.requireNonNull(
        fingerprintCalculator, "fingerprintCalculator is required");
  }

  /**
   * Profiles one tenant directory.
   *
   * @param tenantId tenant identifier
   * @param directoryPath directory path
   * @param encoding source encoding
   * @param mediaType media type
   * @param schema schema identifier
   * @param schemaVersion schema version
   * @param parserHints parser hints
   * @param structuralKeys structural signature keys
   * @param updatedAt profile timestamp
   * @return profile with initial version 1
   */
  public DirectoryMetadataProfile profile(
      String tenantId,
      String directoryPath,
      String encoding,
      String mediaType,
      String schema,
      String schemaVersion,
      Map<String, String> parserHints,
      List<String> structuralKeys,
      Instant updatedAt) {
    FingerprintCalculator.ProfileInput input = new FingerprintCalculator.ProfileInput(
        encoding, mediaType, schema, schemaVersion, parserHints, structuralKeys);
    FormatFingerprint fingerprint = fingerprintCalculator.compute(input);
    return new DirectoryMetadataProfile(
        tenantId,
        directoryPath,
        encoding,
        mediaType,
        schema,
        schemaVersion,
        parserHints,
        fingerprint,
        1,
        updatedAt);
  }
}
