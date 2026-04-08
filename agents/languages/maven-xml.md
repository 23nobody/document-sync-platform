# Maven XML Rules for Agents

## Scope

Applies to `pom.xml`.

## Rules

1. Keep dependency changes minimal and justified by feature or fix scope.
2. Prefer stable, current versions from Maven Central.
3. Do not remove plugin or dependency entries without impact analysis.
4. Preserve reproducible build behavior and test execution defaults.
5. Keep XML clean, consistently indented, and easy to diff.

## Safety Checks

- Run `mvn test` after dependency/plugin changes.
- Ensure no transitive dependency introduces security or license risk.
- Document why new dependency is required.

