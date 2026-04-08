package com.acme.docsync.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.docsync.format.FormatDetectionResult;
import com.acme.docsync.format.FormatParseException;
import com.acme.docsync.format.FormatPlugin;
import com.acme.docsync.format.FormatPluginRegistry;
import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.FormatFingerprint;
import com.acme.docsync.model.ParsedDocument;
import com.acme.docsync.model.SourceDocument;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PluginCanonicalTransformerTest {
  @Test
  void transform_runsPluginPipelineAndValidator() throws Exception {
    FormatPluginRegistry registry = new FormatPluginRegistry();
    registry.register("test", new ValidPlugin());
    PluginCanonicalTransformer transformer = new PluginCanonicalTransformer(
        registry, new CanonicalOutputValidator());

    CanonicalDocument output = transformer.transform(source(), profile());

    assertEquals("tenant1", output.tenantId());
    assertEquals("tenant1/canonical/docs/a.json", output.canonicalPath());
  }

  @Test
  void transform_throwsOnValidationFailure() {
    FormatPluginRegistry registry = new FormatPluginRegistry();
    registry.register("bad", new InvalidPlugin());
    PluginCanonicalTransformer transformer = new PluginCanonicalTransformer(
        registry, new CanonicalOutputValidator());

    TransformException exception = assertThrows(
        TransformException.class,
        () -> transformer.transform(source(), profile()));

    assertEquals(true, exception.getMessage().contains("canonical validation failed"));
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

  private static final class ValidPlugin implements FormatPlugin {
    @Override
    public FormatDetectionResult detect(
        SourceDocument input, DirectoryMetadataProfile profile) {
      return FormatDetectionResult.supported(1);
    }

    @Override
    public ParsedDocument parse(SourceDocument input) throws FormatParseException {
      return new ParsedDocument(Map.of("tenantId", input.tenantId(),
          "sourcePath", input.sourcePath(), "value", "ok"));
    }

    @Override
    public CanonicalDocument map(ParsedDocument parsed) {
      return new CanonicalDocument(
          parsed.fields().get("tenantId").toString(),
          parsed.fields().get("tenantId") + "/canonical/"
              + parsed.fields().get("sourcePath"),
          parsed.fields());
    }

    @Override
    public void validate(CanonicalDocument canonical) {
      // no-op
    }
  }

  private static final class InvalidPlugin implements FormatPlugin {
    @Override
    public FormatDetectionResult detect(
        SourceDocument input, DirectoryMetadataProfile profile) {
      return FormatDetectionResult.supported(1);
    }

    @Override
    public ParsedDocument parse(SourceDocument input) {
      return new ParsedDocument(Map.of("tenantId", "", "sourcePath", ""));
    }

    @Override
    public CanonicalDocument map(ParsedDocument parsed) {
      return new CanonicalDocument("tenant1", "", Map.of());
    }

    @Override
    public void validate(CanonicalDocument canonical) {
      // no-op
    }
  }
}
