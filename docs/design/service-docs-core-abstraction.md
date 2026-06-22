# Service Docs Core Abstraction

## Goal

Move service-docs document indexing rules into a Spring-free core abstraction, while keeping HTTP exposure and classpath resource scanning in Spring MVC / Spring Boot adapter code.

## Current Behavior

- Markdown documents live under `resources/service-docs/**/*.md`.
- OpenAPI JSON lives at `resources/service-docs/api/openapi.json`.
- The public index excludes `service-docs/api/openapi.json` because API documents are synchronized to zyplayer-doc as API interface documents, not as a Markdown page.
- Document ids are relative paths under `service-docs/` without the `.md` suffix, for example `guide/token`.
- Invalid ids are rejected when blank, containing `..`, or containing `\`.

## Proposed Split

- `isass-web-springmvc` keeps HTTP controller code and Spring `ResourcePatternResolver` scanning.
- A new Spring-free class should own path normalization, doc id validation, title extraction, type extraction, and OpenAPI path constants.
- The Spring scanner should pass discovered resource path and content-reader callbacks into that pure Java class.

## Future Candidate Classes

- `ServiceDocsPaths`: constants and id normalization.
- `ServiceDocsIndex`: pure Java functions that convert resource paths to `ServiceDoc`.
- `ServiceDocsContentReader`: small interface used by Spring scanner if needed.

## Non-Goals

- Do not move HTTP controller code out of `isass-web-springmvc` in the first refactor.
- Do not add a new module until there are at least two non-Spring consumers.
- Do not change public URLs.
- Do not change service-docs directory names.

## Verification

- Existing `ServiceDocsScannerTest` and `ServiceDocsControllerTest` must keep passing.
- Attachment must still expose `/attachment-service/service-docs` and `/attachment-service/v3/api-docs`.
