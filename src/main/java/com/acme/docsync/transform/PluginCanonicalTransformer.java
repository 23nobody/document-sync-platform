package com.acme.docsync.transform;

import com.acme.docsync.format.FormatParseException;
import com.acme.docsync.format.FormatPlugin;
import com.acme.docsync.format.FormatPluginRegistry;
import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.ParsedDocument;
import com.acme.docsync.model.SourceDocument;
import java.util.Objects;

/**
 * Canonical transformer backed by plugin registry and output validator.
 */
public final class PluginCanonicalTransformer implements CanonicalTransformer {
  private final FormatPluginRegistry pluginRegistry;
  private final CanonicalOutputValidator outputValidator;

  /**
   * Creates a plugin-backed transformer.
   *
   * @param pluginRegistry format plugin registry
   * @param outputValidator canonical output validator
   */
  public PluginCanonicalTransformer(
      FormatPluginRegistry pluginRegistry,
      CanonicalOutputValidator outputValidator) {
    this.pluginRegistry = Objects.requireNonNull(
        pluginRegistry, "pluginRegistry is required");
    this.outputValidator = Objects.requireNonNull(
        outputValidator, "outputValidator is required");
  }

  @Override
  public CanonicalDocument transform(SourceDocument input, DirectoryMetadataProfile profile)
      throws TransformException {
    try {
      FormatPlugin plugin = pluginRegistry.resolve(input, profile);
      ParsedDocument parsed = plugin.parse(input);
      CanonicalDocument canonical = plugin.map(parsed);
      plugin.validate(canonical);

      CanonicalValidationResult result = outputValidator.validate(canonical);
      if (!result.valid()) {
        throw new TransformException(
            "canonical validation failed: " + String.join(", ", result.violations()),
            null);
      }
      return canonical;
    } catch (FormatParseException e) {
      throw new TransformException("failed to parse source document", e);
    }
  }
}
