package com.acme.docsync.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.docsync.storage.TenantPathPolicy;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class IngestionContractValidatorTest {
  @Test
  void normalizeAndValidate_normalizesTenantAndPath() {
    IngestionContractValidator validator = new IngestionContractValidator(
        new TenantPathPolicy());
    IngestionContract input = new IngestionContract(
        " Tenant_One ",
        " docs/file.json ",
        IngestionOperation.ADD,
        "v1",
        Instant.parse("2026-01-01T00:00:00Z"),
        "source-a",
        "req-1");

    IngestionContract normalized = validator.normalizeAndValidate(input);

    assertEquals("tenant_one", normalized.tenantId());
    assertEquals("docs/file.json", normalized.path());
  }

  @Test
  void normalizeAndValidate_rejectsMissingOperation() {
    IngestionContractValidator validator = new IngestionContractValidator(
        new TenantPathPolicy());
    IngestionContract input = new IngestionContract(
        "tenant1",
        "docs/file.json",
        null,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
        null,
        null);

    IngestionValidationException exception = assertThrows(
        IngestionValidationException.class,
        () -> validator.normalizeAndValidate(input));

    assertEquals("operation is required", exception.getMessage());
  }

  @Test
  void normalizeAndValidate_rejectsPathTraversal() {
    IngestionContractValidator validator = new IngestionContractValidator(
        new TenantPathPolicy());
    IngestionContract input = new IngestionContract(
        "tenant1",
        "../secrets.txt",
        IngestionOperation.EDIT,
        null,
        Instant.parse("2026-01-01T00:00:00Z"),
        null,
        null);

    IngestionValidationException exception = assertThrows(
        IngestionValidationException.class,
        () -> validator.normalizeAndValidate(input));

    assertEquals("path traversal is not allowed", exception.getMessage());
  }
}
