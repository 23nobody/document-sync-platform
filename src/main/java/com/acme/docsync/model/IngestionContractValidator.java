package com.acme.docsync.model;

import com.acme.docsync.storage.TenantPathPolicy;
import java.util.Objects;

/**
 * Validates and normalizes ingestion contracts.
 */
public final class IngestionContractValidator {
  private final TenantPathPolicy tenantPathPolicy;

  /**
   * Creates a contract validator.
   *
   * @param tenantPathPolicy tenant and path safety policy
   */
  public IngestionContractValidator(TenantPathPolicy tenantPathPolicy) {
    this.tenantPathPolicy = Objects.requireNonNull(
        tenantPathPolicy, "tenantPathPolicy is required");
  }

  /**
   * Validates required fields and returns a normalized contract.
   *
   * @param contract candidate contract
   * @return normalized contract
   */
  public IngestionContract normalizeAndValidate(IngestionContract contract) {
    if (contract == null) {
      throw new IngestionValidationException("contract is required");
    }

    String normalizedTenantId = tenantPathPolicy.normalizeTenantId(
        contract.tenantId());
    tenantPathPolicy.assertRelativePathSafe(contract.path());

    if (contract.operation() == null) {
      throw new IngestionValidationException("operation is required");
    }
    if (contract.timestamp() == null) {
      throw new IngestionValidationException("timestamp is required");
    }

    return new IngestionContract(
        normalizedTenantId,
        contract.path().trim(),
        contract.operation(),
        contract.etagOrVersion(),
        contract.timestamp(),
        contract.sourceId(),
        contract.requestId());
  }
}
