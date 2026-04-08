package com.acme.docsync.format;

import com.acme.docsync.model.DirectoryMetadataProfile;
import com.acme.docsync.model.SourceDocument;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Allow-list plugin registry and resolver.
 */
public final class FormatPluginRegistry {
  private final Map<String, FormatPlugin> plugins = new HashMap<>();

  /**
   * Registers a plugin by stable ID.
   *
   * @param pluginId plugin identifier
   * @param plugin plugin instance
   */
  public void register(String pluginId, FormatPlugin plugin) {
    Objects.requireNonNull(pluginId, "pluginId is required");
    Objects.requireNonNull(plugin, "plugin is required");
    if (plugins.containsKey(pluginId)) {
      throw new IllegalArgumentException("duplicate pluginId: " + pluginId);
    }
    plugins.put(pluginId, plugin);
  }

  /**
   * Resolves best plugin for input and profile.
   *
   * @param input source document
   * @param profile directory metadata profile
   * @return resolved plugin
   */
  public FormatPlugin resolve(SourceDocument input, DirectoryMetadataProfile profile) {
    Objects.requireNonNull(input, "input is required");
    Objects.requireNonNull(profile, "profile is required");

    FormatPlugin best = null;
    double bestScore = -1;
    for (FormatPlugin plugin : plugins.values()) {
      FormatDetectionResult result = plugin.detect(input, profile);
      if (result.supported() && result.score() > bestScore) {
        best = plugin;
        bestScore = result.score();
      }
    }
    if (best == null) {
      throw new IllegalStateException("no matching format plugin for input");
    }
    return best;
  }
}
