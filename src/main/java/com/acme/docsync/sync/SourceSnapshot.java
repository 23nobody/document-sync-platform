package com.acme.docsync.sync;

import java.time.Instant;
import java.util.Map;

/**
 * Captured source state for a tenant/source root.
 *
 * @param tenantId tenant identifier
 * @param sourceId source identifier
 * @param capturedAt capture time
 * @param filesByPath map of relative path to last-modified instant
 */
public record SourceSnapshot(
    String tenantId,
    String sourceId,
    Instant capturedAt,
    Map<String, Instant> filesByPath
) {
}
