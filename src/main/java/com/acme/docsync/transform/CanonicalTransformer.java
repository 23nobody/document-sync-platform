package com.acme.docsync.transform;

import com.acme.docsync.model.CanonicalDocument;
import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.SourceDocument;

/**
 * Produces canonical output via plugin parse/map/validate chain.
 */
public interface CanonicalTransformer {
  /**
   * Transforms source document into canonical output.
   *
   * @param input source document
   * @param profile directory metadata profile
   * @return canonical output
   * @throws TransformException if transformation fails
   */
  CanonicalDocument transform(SourceDocument input, DirectoryMetadataProfile profile)
      throws TransformException;
}
