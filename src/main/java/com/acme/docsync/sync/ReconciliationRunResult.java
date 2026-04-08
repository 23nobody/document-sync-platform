package com.acme.docsync.sync;

/**
 * Result summary for one reconciliation run.
 *
 * @param createdCount number of created-path events emitted
 * @param modifiedCount number of modified-path events emitted
 * @param deletedCount number of deleted-path events emitted
 * @param failedSources number of source roots that failed
 */
public record ReconciliationRunResult(
    int createdCount,
    int modifiedCount,
    int deletedCount,
    int failedSources
) {
}
