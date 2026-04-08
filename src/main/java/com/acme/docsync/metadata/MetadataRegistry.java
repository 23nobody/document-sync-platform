package com.acme.docsync.metadata;

import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.MetadataVersionRecord;
import java.util.List;

/**
 * Directory-level metadata registry contract.
 */
public interface MetadataRegistry {
  /**
   * Upserts a metadata profile.
   *
   * @param profile directory metadata profile
   */
  void upsertProfile(DirectoryMetadataProfile profile);

  /**
   * Gets latest profile for a tenant directory.
   *
   * @param tenantId tenant identifier
   * @param directoryPath directory path
   * @return latest profile or null
   */
  DirectoryMetadataProfile getLatest(String tenantId, String directoryPath);

  /**
   * Returns profile version history for a tenant directory.
   *
   * @param tenantId tenant identifier
   * @param directoryPath directory path
   * @return ordered version history
   */
  List<MetadataVersionRecord> history(String tenantId, String directoryPath);
}
