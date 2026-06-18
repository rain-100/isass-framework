# Agent Instructions

## ChangeLog

- Any change that affects code, configuration, dependencies, generated artifacts, database migration behavior, or user-facing documentation must be recorded in `docs/60.changelog/ChangeLog4.x.md`.
- Keep entries concise and consistent with the existing changelog style.
- Update the changelog in the same change set as the implementation, not as a separate follow-up.

## Build Verification

- After framework changes, run `mvn install` from the repository root so downstream projects can depend on the latest local framework build.
- If dependency resolution depends on Maven Central rather than the configured mirror, mention the command/settings used in the final response.
