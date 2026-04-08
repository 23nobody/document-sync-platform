package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionContractValidator;
import com.acme.docsync.model.IngestionOperation;
import com.acme.docsync.model.IngestionValidationException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Maps raw watcher events to normalized ingestion contracts.
 */
public final class WatcherEventMapper {
  private final IngestionContractValidator validator;

  /**
   * Creates a watcher event mapper.
   *
   * @param validator ingestion contract validator
   */
  public WatcherEventMapper(IngestionContractValidator validator) {
    this.validator = Objects.requireNonNull(validator, "validator is required");
  }

  /**
   * Converts a raw watcher event to validated ingestion contract.
   *
   * @param event raw watcher event
   * @param now fallback timestamp when event timestamp is missing
   * @return normalized ingestion event
   */
  public IngestionContract toIngestionEvent(RawWatcherEvent event, Instant now) {
    if (event == null) {
      throw new IngestionValidationException("raw watcher event is required");
    }
    if (event.relativePath() == null) {
      throw new IngestionValidationException("relativePath is required");
    }
    Instant timestamp = event.occurredAt() != null ? event.occurredAt() : now;
    if (timestamp == null) {
      throw new IngestionValidationException("timestamp is required");
    }

    IngestionOperation operation = switch (event.kind()) {
      case CREATE -> IngestionOperation.ADD;
      case MODIFY -> IngestionOperation.EDIT;
      case DELETE -> IngestionOperation.DELETE;
    };

    String relativePath = safePath(event.relativePath());
    IngestionContract contract = new IngestionContract(
        event.tenantId(),
        relativePath,
        operation,
        null,
        timestamp,
        event.sourceId(),
        null);
    return validator.normalizeAndValidate(contract);
  }

  private String safePath(Path relativePath) {
    String normalized = relativePath.normalize().toString();
    if (normalized.isBlank()) {
      throw new IngestionValidationException("relativePath is required");
    }
    return normalized.replace('\\', '/');
  }
}
