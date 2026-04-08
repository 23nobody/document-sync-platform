# Java Test Rules for Agents

## Scope

Applies to `src/test/java/**/*.java`.

## Rules

1. Keep tests deterministic and isolated; avoid time/order coupling.
2. Name tests by behavior and expected outcome.
3. Cover happy path, validation failures, and edge cases.
4. Add tests for idempotency (`ADD`, `EDIT`, `DELETE`) when sync logic changes.
5. Add reconciliation and delete-consistency tests when relevant modules change.
6. Use clear arrange-act-assert structure with minimal duplication.

## Required Assertions

- Assert tenant boundary safety for multi-tenant flows.
- Assert actionable error messages for failure paths.
- Assert canonical output validation behavior when mappings change.

