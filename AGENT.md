# AGENT.md - System-Safe Project Guidance

This file defines mandatory behavior for AI/code agents in this repository.

## Mission

Build and maintain a multi-tenant document sync platform that is safe,
idempotent, and operationally predictable.

## Hard Safety Rules (Non-Negotiable)

1. Never cross tenant boundaries in storage, metadata, processing, or output.
2. Never remove or weaken validation, auditability, or provenance guarantees.
3. Never perform destructive operations (mass delete/reset/drop) without explicit
   user approval and a rollback plan.
4. Never expose secrets, credentials, private keys, or sensitive data in logs,
   code, tests, or docs.
5. Never bypass idempotency checks for `ADD`, `EDIT`, or `DELETE` operations.
6. Never silently swallow exceptions; return actionable errors with context.
7. Never change canonical schema behavior without updating validator logic and
   tests in the same change.

## Security and Privacy Controls

- Treat all tenant data as isolated and confidential by default.
- Avoid logging document content; prefer IDs, hashes, and metadata.
- Redact sensitive fields from error messages and debug output.
- Validate all external/parsing inputs before processing.
- Use allow-list style checks for format/plugin dispatch when feasible.

## Data Integrity and Sync Guarantees

- Preserve at-least-once processing semantics with idempotent handlers.
- Keep event processing and reconciliation behavior consistent.
- Ensure delete semantics are consistent between canonical store and index.
- Keep provenance mapping (`source path -> canonical path`) complete and
  queryable.
- Maintain deterministic canonical output for equivalent inputs.

## Implementation Standards

- Prefer small, focused changes over broad refactors.
- Keep module boundaries aligned with existing package responsibilities:
  `storage`, `metadata`, `sync`, `format`, `transform`, `model`, `app`.
- Add or update tests for behavior changes, especially:
  - tenant isolation
  - idempotency
  - reconciliation correctness
  - canonical validation
  - delete consistency
- Preserve backward compatibility for contracts unless explicitly requested.

## Operational Rules for Agents

- Before editing, read relevant files and infer local conventions.
- If requirements are ambiguous, ask concise clarifying questions first.
- If a safety rule conflicts with a requested change, stop and call it out.
- Document assumptions in PR/commit notes when behavior is inferred.
- Prefer reversible migrations and include fallback notes for risky changes.

## Sub-Agent Topology

Use role-specialized sub-agents for non-trivial work:

- `Planner`: define scope, assumptions, and an execution plan.
- `Architect`: produce module-safe design and invariants.
- `Coder`: implement scoped changes and tests.
- `Reviewer`: run risk-first review and block on severe issues.
- `QA`: execute validation scenarios and provide `go`/`no-go`.

Preferred flow:

1. Planner -> 2. Architect -> 3. Coder -> 4. Reviewer -> 5. QA

Handoff rule:

- Each agent must consume previous outputs and preserve all safety constraints
  from this file.

## Language-Specific Rules

All agents must apply language/file specific rules before proposing or making
changes:

- Java source: `agents/languages/java.md`
- Java tests: `agents/languages/java-tests.md`
- Maven XML: `agents/languages/maven-xml.md`
- Markdown/docs: `agents/languages/markdown.md`

## Change Checklist

For non-trivial changes, verify:

- [ ] Tenant isolation preserved end-to-end.
- [ ] Idempotency maintained for all operation types.
- [ ] Validation and provenance logic still enforced.
- [ ] Tests added/updated and passing (`mvn test`).
- [ ] No secret leakage in code/logs/tests/docs.
- [ ] Error paths are explicit and actionable.

