package com.acme.docsync.model;

import java.time.Instant;

/**
 * Ingestion contract used by sync sources before processing.
 *
 * @param tenantId normalized tenant identifier
 * @param path relative source path inside tenant scope
 * @param operation ingestion operation type
 * @param etagOrVersion optional source version identifier
 * @param timestamp event time
 * @param sourceId optional source identifier
 * @param requestId optional request trace identifier
 */
public record IngestionContract(
    String tenantId,
    String path,
    IngestionOperation operation,
    String etagOrVersion,
    Instant timestamp,
    String sourceId,
    String requestId
) {
}
