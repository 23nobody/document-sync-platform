package com.acme.docsync.metadata;

import com.acme.docsync.model.FormatFingerprint;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Calculates deterministic structural format fingerprints.
 */
public final class FingerprintCalculator {
  private static final String ALGORITHM = "SHA-256";

  /**
   * Computes a stable fingerprint from profile inputs.
   *
   * @param input profile input
   * @return computed fingerprint
   */
  public FormatFingerprint compute(ProfileInput input) {
    Objects.requireNonNull(input, "input is required");
    String canonical = canonicalize(input);
    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
      return new FormatFingerprint(toHex(hash), ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("missing hash algorithm: " + ALGORITHM, e);
    }
  }

  private String canonicalize(ProfileInput input) {
    StringBuilder builder = new StringBuilder();
    builder.append(input.encoding()).append('|');
    builder.append(input.mediaType()).append('|');
    builder.append(input.schema()).append('|');
    builder.append(input.schemaVersion()).append('|');

    Map<String, String> sortedHints = new TreeMap<>(input.parserHints());
    for (Map.Entry<String, String> entry : sortedHints.entrySet()) {
      builder.append("hint:").append(entry.getKey()).append('=')
          .append(entry.getValue()).append(';');
    }

    List<String> sortedKeys = input.structuralKeys().stream().sorted().toList();
    for (String key : sortedKeys) {
      builder.append("key:").append(key).append(';');
    }
    return builder.toString();
  }

  private String toHex(byte[] data) {
    StringBuilder builder = new StringBuilder(data.length * 2);
    for (byte value : data) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }

  /**
   * Fingerprint input shape.
   *
   * @param encoding source encoding
   * @param mediaType media type
   * @param schema schema identifier
   * @param schemaVersion schema version
   * @param parserHints parser hints
   * @param structuralKeys structural key signature
   */
  public record ProfileInput(
      String encoding,
      String mediaType,
      String schema,
      String schemaVersion,
      Map<String, String> parserHints,
      List<String> structuralKeys
  ) {
    public ProfileInput {
      Objects.requireNonNull(encoding, "encoding is required");
      Objects.requireNonNull(mediaType, "mediaType is required");
      Objects.requireNonNull(schema, "schema is required");
      Objects.requireNonNull(schemaVersion, "schemaVersion is required");
      Objects.requireNonNull(parserHints, "parserHints is required");
      Objects.requireNonNull(structuralKeys, "structuralKeys is required");
      parserHints = Map.copyOf(parserHints);
      structuralKeys = List.copyOf(structuralKeys);
    }
  }
}
