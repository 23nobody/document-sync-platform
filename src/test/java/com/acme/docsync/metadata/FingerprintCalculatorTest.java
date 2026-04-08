package com.acme.docsync.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acme.docsync.model.FormatFingerprint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FingerprintCalculatorTest {
  @Test
  void compute_isDeterministicForEquivalentInputs() {
    FingerprintCalculator calculator = new FingerprintCalculator();
    FingerprintCalculator.ProfileInput left = new FingerprintCalculator.ProfileInput(
        "utf-8",
        "application/json",
        "doc-schema",
        "1.0",
        Map.of("mode", "strict", "date", "iso8601"),
        List.of("b", "a"));
    FingerprintCalculator.ProfileInput right = new FingerprintCalculator.ProfileInput(
        "utf-8",
        "application/json",
        "doc-schema",
        "1.0",
        Map.of("date", "iso8601", "mode", "strict"),
        List.of("a", "b"));

    FormatFingerprint one = calculator.compute(left);
    FormatFingerprint two = calculator.compute(right);

    assertEquals(one.value(), two.value());
    assertEquals("SHA-256", one.algorithm());
  }
}
