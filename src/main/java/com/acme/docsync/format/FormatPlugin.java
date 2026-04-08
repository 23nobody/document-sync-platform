package com.acme.docsync.format;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.ParsedDocument;
import com.acme.docsync.model.SourceDocument;
import com.acme.docsync.transform.TransformException;

/**
 * Extension point for parsing new document formats.
 */
public interface FormatPlugin {
  /**
   * Detects whether this plugin supports the source input.
   *
   * @param input source document
   * @param profile directory metadata profile
   * @return detection result
   */
  FormatDetectionResult detect(SourceDocument input, DirectoryMetadataProfile profile);

  /**
   * Parses source payload into intermediate representation.
   *
   * @param input source document
   * @return parsed document
   * @throws FormatParseException if payload cannot be parsed
   */
  ParsedDocument parse(SourceDocument input) throws FormatParseException;

  /**
   * Maps parsed document to canonical output shape.
   *
   * @param parsed parsed document
   * @return canonical document
   * @throws TransformException if mapping fails
   */
  CanonicalDocument map(ParsedDocument parsed) throws TransformException;

  /**
   * Validates canonical output produced by the plugin.
   *
   * @param canonical canonical document
   * @throws TransformException when validation fails
   */
  void validate(CanonicalDocument canonical) throws TransformException;
}
