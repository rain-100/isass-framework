# Service Docs Apidoc Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expose each microservice's Markdown files from `src/main/resources/service-docs/` and let `isass-service-apidoc` aggregate service API and Markdown docs.

**Architecture:** `isass-web-springmvc` owns the reusable `/service-docs` contract. Business services only package Markdown and optional screw generation config. `isass-service-apidoc` discovers services through Nacos and pulls `/v3/api-docs` plus `/service-docs`.

**Tech Stack:** Java 25, Spring Boot 4, Spring WebMVC, Maven 4, Nacos Discovery, screw Maven plugin.

---

### Task 1: Framework Service Docs Contract

**Files:**
- Create: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDoc.java`
- Create: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDocsScanner.java`
- Create: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDocsController.java`
- Test: `isass-web-springmvc/src/test/java/vip/isass/framework/web/servicedocs/ServiceDocsScannerTest.java`

- [ ] Write a failing scanner test for nested Markdown files.
- [ ] Implement scanner and immutable DTO.
- [ ] Add controller for `GET /service-docs` and `GET /service-docs/{docId}`.
- [ ] Run `mvn -pl isass-web-springmvc test`.

### Task 2: Attachment Service Documents

**Files:**
- Create: `isass-service-attachment-service/src/main/resources/service-docs/database/attachment-db.md`
- Modify: `isass-service-attachment-service/pom.xml`

- [ ] Add a stable Markdown database document placeholder.
- [ ] Add a disabled-by-default `db-doc` Maven profile for manual screw generation.
- [ ] Run a service compile/test command that does not require a database.

### Task 3: Apidoc Aggregation Baseline

**Files:**
- Modify: `pom.xml`
- Modify: `isass-service-apidoc-service/pom.xml`
- Create aggregation service/controller classes under `isass-service-apidoc-service/src/main/java`.

- [ ] Move apidoc branch to `v4`.
- [ ] Upgrade Maven coordinates to framework v4.
- [ ] Add aggregation endpoints that discover services and expose OpenAPI plus service-docs URLs.
- [ ] Run compile/test for apidoc.
