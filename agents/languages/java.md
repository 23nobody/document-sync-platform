# Java Rules for Agents

## Scope

Applies to `src/main/java/**/*.java`.

## Rules

1. Use Java 17+ compatible syntax and standard library APIs.
2. Add Javadoc for every new public class and public method.
3. Validate constructor and method inputs; fail fast with clear messages.
4. Never swallow exceptions; either handle with context or rethrow typed errors.
5. Keep domain invariants explicit, especially for tenant isolation and
   idempotency.
6. Prefer immutable fields (`final`) and small focused methods.
7. Do not log sensitive document content; log IDs, hashes, and metadata only.

## Error Handling Pattern

- Throw domain-specific exceptions (for example `FormatParseException`).
- Preserve cause chains when wrapping exceptions.
- Return actionable messages without leaking sensitive payload data.

