# Agent Instructions

## CodeGraph

- The parent `isass/.codegraph/` is the workspace index for this project. Use `codegraph_explore` (or `codegraph explore`) before `rg`, `find`, or broad file reads when locating or understanding code.
- Read the current returned source before editing; use `rg` only for follow-up text searches that CodeGraph cannot answer.

## ChangeLog

- Any change that affects code, configuration, dependencies, generated artifacts, database migration behavior, or user-facing documentation must be recorded in `docs/60.changelog/ChangeLog4.x.md`.
- Keep entries concise and consistent with the existing changelog style.
- Update the changelog in the same change set as the implementation, not as a separate follow-up.

## Build Verification

- After framework changes, run `mvn install` from the repository root so downstream projects can depend on the latest local framework build.
- If dependency resolution depends on Maven Central rather than the configured mirror, mention the command/settings used in the final response.

## Framework Conventions

- Framework documentation belongs in `docs/usage/<topic>/`; architecture and design records belong in `docs/design/`; development plans and specifications belong in `docs/superpowers/`.
- `isass-nocode-generator` owns generated model, Criteria, mapper and contract conventions. Change templates under `isass-nocode-generator/src/main/resources/templates/` and regenerate consumers; do not hand-maintain generated output as the long-term fix.
- Application-facing fields and Criteria use Java camelCase properties and lambda references. Database column names are an ORM concern; only add explicit metadata when automatic property-to-column mapping is genuinely ambiguous.
- Nocode supports advanced response projection and association queries. Keep their public query parameters camelCase and document any new behavior in `docs/usage/nocode/`.
- Shared Redis key names use `<microservice>:<domain>:<feature>[:<id>]`. Avoid framework-global key prefixes and avoid clearing unrelated keys.
- Framework configuration uses the `isass.<module>.<feature>...` hierarchy. New reusable configuration must not introduce a one-off root prefix.
- Do not place service-specific initialization data or business rules in framework modules.
