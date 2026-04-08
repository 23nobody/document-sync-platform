package com.acme.docsync.sync;

import java.util.Set;

/**
 * Drift classification between previous and current snapshots.
 *
 * @param created newly created paths
 * @param modified modified paths
 * @param deleted deleted paths
 * @param unchanged unchanged paths
 */
public record DriftResult(
    Set<String> created,
    Set<String> modified,
    Set<String> deleted,
    Set<String> unchanged
) {
}
