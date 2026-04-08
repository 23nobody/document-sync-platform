package com.acme.docsync.sync;

import com.acme.docsync.model.IngestionContract;
import com.acme.docsync.model.IngestionValidationException;
import java.util.Comparator;

/**
 * Compares ingestion events for deterministic processing order.
 */
public final class EventOrderingResolver {
  /**
   * Compares contracts using timestamp precedence for PR1.
   *
   * @param left first contract
   * @param right second contract
   * @return comparator result
   */
  public int compare(IngestionContract left, IngestionContract right) {
    if (left == null || right == null) {
      throw new IngestionValidationException("events to compare must be non-null");
    }
    if (left.timestamp() == null || right.timestamp() == null) {
      throw new IngestionValidationException("event timestamp is required");
    }
    if (left.operation() == null || right.operation() == null) {
      throw new IngestionValidationException("event operation is required");
    }
    if (left.path() == null || right.path() == null) {
      throw new IngestionValidationException("event path is required");
    }

    Comparator<IngestionContract> comparator =
        Comparator.comparing(IngestionContract::timestamp)
            .thenComparing(contract -> contract.operation().name())
            .thenComparing(IngestionContract::path);
    return comparator.compare(left, right);
  }
}
