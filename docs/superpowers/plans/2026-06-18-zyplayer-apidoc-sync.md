# Zyplayer Apidoc Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `isass-apidoc-zyplayer` so each service can sync Markdown and OpenAPI docs to a separately deployed zyplayer-doc V3 instance.

**Architecture:** The new optional module is a Spring Boot auto-configuration starter. It scans `service-docs/**/*.md`, creates or finds a zyplayer space named with the Chinese service name and normalized version, compares managed pages by an `isass-doc-sync` marker, then inserts, updates, skips, releases, or deletes pages through zyplayer OpenAPI.

**Tech Stack:** Java 25, Spring Boot 4 auto-configuration, Spring Web `RestClient`, Jackson, JDK RSA signing, JUnit 5.

---

### Task 1: Sync Domain and Version Naming

**Files:**
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerServiceDescriptor.java`
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerVersion.java`
- Test: `isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/ZyplayerVersionTest.java`

- [ ] Write tests for `4.0.0-SNAPSHOT -> 4.0.0`, `附件微服务v4.0.0`, and `attachment-service:4.0.0`.
- [ ] Implement the descriptor and version utility.
- [ ] Run `mvn -pl isass-apidoc-zyplayer test -Dtest=ZyplayerVersionTest`.

### Task 2: Zyplayer OpenAPI Client

**Files:**
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/client/ZyplayerOpenApiClient.java`
- Create DTO files in `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/client/`
- Test: `isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/client/ZyplayerOpenApiClientTest.java`

- [ ] Write a mock HTTP server test that verifies `key`, `signature`, and JSON `content` are submitted as zyplayer expects.
- [ ] Implement JDK RSA SHA256 signing and form POST calls.
- [ ] Run the client tests.

### Task 3: Sync Planner

**Files:**
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/sync/ZyplayerDocSyncService.java`
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/sync/ZyplayerSyncMarker.java`
- Test: `isass-apidoc-zyplayer/src/test/java/vip/isass/framework/apidoc/zyplayer/sync/ZyplayerDocSyncServiceTest.java`

- [ ] Write tests for find/create space, skip unchanged pages, update changed pages, insert missing pages, and delete missing managed pages only when enabled.
- [ ] Implement sync logic using stable markers.
- [ ] Run sync service tests.

### Task 4: Spring Boot Auto-Configuration

**Files:**
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerApidocAutoConfiguration.java`
- Create: `isass-apidoc-zyplayer/src/main/java/vip/isass/framework/apidoc/zyplayer/ZyplayerApidocProperties.java`
- Create: `isass-apidoc-zyplayer/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: root `pom.xml`

- [ ] Register module in the framework build.
- [ ] Enable auto-sync after application ready when `isass.apidoc.zyplayer.enabled=true`.
- [ ] Keep `info.service-name-cn` as the first Chinese name source.

### Task 5: Attachment Integration and Documentation

**Files:**
- Modify: `isass-service-attachment/**/pom.xml`
- Modify: `isass-framework-v4/README.md`

- [ ] Replace attachment's apidoc-service dependency with `isass-apidoc-zyplayer`.
- [ ] Document zyplayer-doc V3 deployment, config, versioned space names, and future `pom service-name-cn` support.
- [ ] Run framework and attachment compile checks.
