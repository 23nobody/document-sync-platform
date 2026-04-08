package com.acme.docsync.sync;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Detects file-level drift between current and previous snapshots.
 */
public final class DriftDetector {
  /**
   * Computes drift result for two snapshots.
   *
   * @param current current snapshot
   * @param previous previous snapshot, nullable
   * @return drift result
   */
  public DriftResult detect(SourceSnapshot current, SourceSnapshot previous) {
    Map<String, Instant> currentFiles = current.filesByPath();
    Map<String, Instant> previousFiles =
        previous == null ? Map.of() : previous.filesByPath();

    Set<String> created = new HashSet<>();
    Set<String> modified = new HashSet<>();
    Set<String> deleted = new HashSet<>();
    Set<String> unchanged = new HashSet<>();

    for (Map.Entry<String, Instant> entry : currentFiles.entrySet()) {
      String path = entry.getKey();
      Instant currentTs = entry.getValue();
      Instant previousTs = previousFiles.get(path);
      if (previousTs == null) {
        created.add(path);
      } else if (!currentTs.equals(previousTs)) {
        modified.add(path);
      } else {
        unchanged.add(path);
      }
    }
    for (String path : previousFiles.keySet()) {
      if (!currentFiles.containsKey(path)) {
        deleted.add(path);
      }
    }
    return new DriftResult(created, modified, deleted, unchanged);
  }
}
