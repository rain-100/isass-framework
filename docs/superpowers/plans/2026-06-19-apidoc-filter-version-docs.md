# API Doc Filter Version Docs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move API doc knowledge out of the root README, add zyplayer filter/group/version support, and improve attachment smart-doc Javadocs.

**Architecture:** Keep `isass-apidoc-zyplayer` as the Spring Boot integration module. Filtering happens before OpenAPI operations are converted to zyplayer API pages. Zyplayer grouping and version metadata are modeled in the sync layer so callers only pass service descriptors and documents.

**Tech Stack:** Java 25, Spring Boot configuration properties, Jackson 3, JUnit 5, AssertJ, smart-doc, zyplayer-doc OpenAPI.

---

### Task 1: Move API Doc Knowledge To Docs

**Files:**
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/README.md`
- Create: `/Users/rain/a/code/company/isass/isass-framework-v4/docs/design/apidoc/zyplayer-doc.md`
- Create: `/Users/rain/a/code/company/isass/isass-framework-v4/docs/usage/apidoc/smart-doc.md`
- Create: `/Users/rain/a/code/company/isass/isass-framework-v4/docs/usage/apidoc/zyplayer-doc.md`

- [ ] Keep README as a short navigation section pointing to the three docs.
- [ ] Move editorType, directory, space, group, version, and sync details into zyplayer docs.
- [ ] Move screw/smart-doc/service-docs/Javadoc usage into smart-doc usage docs.

### Task 2: Add OpenAPI Exclude Rules

**Files:**
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerApidocProperties.java`
- Create: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/openapi/ZyplayerOpenApiExcludeRules.java`
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/openapi/ZyplayerOpenApiDocumentConverter.java`
- Test: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/openapi/ZyplayerOpenApiExcludeRulesTest.java`
- Test: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/openapi/ZyplayerOpenApiDocumentConverterTest.java`

- [ ] Write tests for exact `url`, exact `METHOD url`, pattern `url`, and pattern `METHOD url` exclusions.
- [ ] Implement default framework excludes for `IsassErrorController` and error paths.
- [ ] Bind business excludes from configuration.

### Task 3: Add Zyplayer Group And Space Version Metadata

**Files:**
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerApidocProperties.java`
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerServiceDescriptor.java`
- Modify: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/sync/ZyplayerDocSyncService.java`
- Test: `/Users/rain/a/code/company/isass/isass-framework-v4/isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/sync/ZyplayerDocSyncServiceTest.java`

- [ ] Change space name to service Chinese name and keep a stable space uuid.
- [ ] Add `groupName` configuration, defaulting to `isass`.
- [ ] ~~Enable zyplayer space version control metadata and pass the cleaned service version for page sync where the OpenAPI supports it.~~ (2026-06-26: removed)

### Task 4: Improve Attachment Javadocs

**Files:**
- Modify controller files under `/Users/rain/a/code/company/isass/isass-service-attachment/isass-service-attachment-service/src/main/java/vip/isass/attachment/controller/`
- Modify custom DTO/entity/API files referenced by controller method parameters and returns under `/Users/rain/a/code/company/isass/isass-service-attachment`

- [ ] Add Chinese `@param` and `@return` to controller methods.
- [ ] Add field Javadocs to custom request/response entities used by those methods.
- [ ] Regenerate smart-doc output into `src/main/resources/service-docs/api/`.

### Task 5: Verify

**Commands:**
- `MAVEN_OPTS=-Daether.syncContext.named.factory=rwlock-local mvn -pl isass-apidoc-zyplayer test`
- `MAVEN_OPTS=-Daether.syncContext.named.factory=rwlock-local mvn -pl isass-apidoc-zyplayer install -DskipTests`
- `MAVEN_OPTS=-Daether.syncContext.named.factory=rwlock-local mvn -pl isass-service-attachment-service -Psmart-doc generate-resources`
- Start attachment and verify zyplayer sync logs.
