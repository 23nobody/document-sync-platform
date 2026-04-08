package com.acme.docsync.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.FormatFingerprint;
import com.acme.docsync.model.ParsedDocument;
import com.acme.docsync.model.SourceDocument;
import com.acme.docsync.transform.TransformException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FormatPluginRegistryTest {
  @Test
  void resolve_returnsHighestScoringSupportedPlugin() {
    FormatPluginRegistry registry = new FormatPluginRegistry();
    registry.register("p1", new TestPlugin(0.3, true));
    registry.register("p2", new TestPlugin(0.8, true));
    SourceDocument source = new SourceDocument(
        "tenant1", "docs/a.json", "application/json", "{}".getBytes(StandardCharsets.UTF_8));
    DirectoryMetadataProfile profile = profile();

    FormatPlugin resolved = registry.resolve(source, profile);

    CanonicalDocument mapped = map(resolved, source);
    assertEquals("tenant1/canonical/docs/a.json", mapped.canonicalPath());
  }

  @Test
  void resolve_throwsWhenNoPluginSupportsInput() {
    FormatPluginRegistry registry = new FormatPluginRegistry();
    registry.register("p1", new TestPlugin(0.2, false));

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> registry.resolve(source(), profile()));

    assertEquals("no matching format plugin for input", exception.getMessage());
  }

  private CanonicalDocument map(FormatPlugin plugin, SourceDocument source) {
    try {
      return plugin.map(plugin.parse(source));
    } catch (FormatParseException | TransformException e) {
      throw new IllegalStateException("unexpected test error", e);
    }
  }

  private SourceDocument source() {
    return new SourceDocument(
        "tenant1", "docs/a.json", "application/json", "{}".getBytes(StandardCharsets.UTF_8));
  }

  private DirectoryMetadataProfile profile() {
    return new DirectoryMetadataProfile(
        "tenant1",
        "docs",
        "utf-8",
        "application/json",
        "schema-doc",
        "1.0",
        Map.of(),
        new FormatFingerprint("abc", "SHA-256"),
        1,
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  private static final class TestPlugin implements FormatPlugin {
    private final double score;
    private final boolean supported;

    private TestPlugin(double score, boolean supported) {
      this.score = score;
      this.supported = supported;
    }

    @Override
    public FormatDetectionResult detect(
        SourceDocument input, DirectoryMetadataProfile profile) {
      return supported ? FormatDetectionResult.supported(score)
          : FormatDetectionResult.unsupported();
    }

    @Override
    public ParsedDocument parse(SourceDocument input) {
      return new ParsedDocument(Map.of("sourcePath", input.sourcePath(), "tenantId",
          input.tenantId()));
    }

    @Override
    public CanonicalDocument map(ParsedDocument parsed) {
      String sourcePath = parsed.fields().get("sourcePath").toString();
      String tenantId = parsed.fields().get("tenantId").toString();
      return new CanonicalDocument(
          tenantId,
          tenantId + "/canonical/" + sourcePath,
          parsed.fields());
    }

    @Override
    public void validate(CanonicalDocument canonical) {
      // no-op
    }
  }
}
