# OpenAPI 3 Resource Path Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the obsolete Markdown service-docs runtime and hard-switch smart-doc/OpenAPI loading to `openapi3/openapi.json`.

**Architecture:** `ServiceDocsController` becomes the sole framework OpenAPI endpoint owner: it reads `classpath:/openapi3/openapi.json`, optionally passes the raw JSON to `OpenApiEnhancerSpi`, and caches the final result with double-checked locking. R2 supplies a stateless SPI adapter around `V3OpenApiEnhancer`; R3 generates the resource at the new path and removes the old `service-docs` tree while preserving screw configuration.

**Tech Stack:** Java 25, Spring Boot 4.0.6, Spring MVC, Jackson 3, Maven 4.1.0, smart-doc 3.1.2.

---

### Task 1: Framework OpenAPI endpoint and cache

**Files:**
- Modify: `isass-web-springmvc/src/test/java/vip/isass/framework/web/servicedocs/ServiceDocsControllerTest.java`
- Move: `isass-web-springmvc/src/test/resources/service-docs/api/openapi.json` → `isass-web-springmvc/src/test/resources/openapi3/openapi.json`
- Modify: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/OpenApiEnhancerSpi.java`
- Modify: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDocsController.java`
- Delete: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDocsScanner.java`
- Delete: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDocsPaths.java`
- Delete: `isass-web-springmvc/src/main/java/vip/isass/framework/web/servicedocs/ServiceDoc.java`
- Delete: `isass-web-springmvc/src/test/java/vip/isass/framework/web/servicedocs/ServiceDocsScannerTest.java`
- Modify: `isass-web-springmvc/src/main/java/vip/isass/framework/web/WebAutoConfiguration.java`

- [ ] **Step 1: Write failing controller tests**

Change the test fixture to `openapi3/openapi.json`. Construct the controller with a
`ResourceLoader` and `ObjectProvider<OpenApiEnhancerSpi>`. Assert:

```java
assertThat(controller.openApi().getBody()).contains("\"openapi\":\"3.1.0\"");
verify(enhancer, times(1)).enhance(anyString());
assertThat(controller.openApi().getBody()).isSameAs(firstBody);
```

Add a retry test using an enhancer that throws once and succeeds on the second call.
Remove tests for Markdown indexing and content endpoints.

- [ ] **Step 2: Run the test and verify RED**

Run:

```bash
mvn -pl isass-web-springmvc test -Dtest=ServiceDocsControllerTest \
  -Dmaven.javadoc.skip=true -f pom.xml
```

Expected: compilation/test failure because the old constructor and SPI contract do not match,
or because the controller still reads `service-docs/api/openapi.json`.

- [ ] **Step 3: Implement the endpoint owner**

Use this SPI:

```java
public interface OpenApiEnhancerSpi {
    String enhance(String rawOpenApiJson);
}
```

`ServiceDocsController` must:

```java
private static final String OPEN_API_RESOURCE = "classpath:/openapi3/openapi.json";
private final ResourceLoader resourceLoader;
private final ObjectProvider<OpenApiEnhancerSpi> enhancerProvider;
private final Object cacheLock = new Object();
private volatile String cachedJson;
```

Inside the double-checked lock, read the resource as UTF-8, call
`enhancer.enhance(raw)` when present, assign `cachedJson` only after successful completion,
and map a missing resource to 404. Remove all `/service-docs` mappings.

Delete the scanner/path/model classes and scanner tests. Remove the scanner bean from
`WebAutoConfiguration`.

- [ ] **Step 4: Run framework tests**

Run:

```bash
mvn -pl isass-web-springmvc -am test \
  -Dmaven.javadoc.skip=true -Dsurefire.failIfNoSpecifiedTests=false -f pom.xml
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Update ChangeLog and commit R1**

Record removal of the Markdown runtime and the hard resource-path switch in
`docs/60.changelog/ChangeLog4.x.md`, then commit:

```bash
git add -A
git commit -m "refactor: load cached OpenAPI from openapi3 resource"
```

### Task 2: Stateless apidoc enhancement adapter

**Files:**
- Modify: `isass-service-apidoc-service/src/test/java/vip/isass/apidoc/openapi/OpenApiEnhancerTest.java`
- Modify: `isass-service-apidoc-service/src/main/java/vip/isass/apidoc/openapi/OpenApiEnhancer.java`
- Modify: `isass-service-apidoc-service/src/main/java/vip/isass/apidoc/config/ApidocAutoConfiguration.java`
- Modify: `isass-service-apidoc-service/src/test/java/vip/isass/apidoc/config/ApidocAutoConfigurationTest.java`
- Modify: `isass-service-apidoc-service/src/test/java/vip/isass/apidoc/ApidocE2ETest.java`

- [ ] **Step 1: Write failing stateless-adapter tests**

Replace cache/scanner assertions with:

```java
String result = enhancer.enhance(rawJson);
verify(v3Enhancer).enhance(rawJson, registry);
assertThat(result).isEqualTo(enhancedJson);
```

Update e2e test configuration to expose `openapi3/openapi.json` through a test resource
instead of mocking `ServiceDocsScanner`.

- [ ] **Step 2: Run the tests and verify RED**

Run:

```bash
mvn -pl isass-service-apidoc-service test \
  -Dtest=OpenApiEnhancerTest,ApidocAutoConfigurationTest,ApidocE2ETest \
  -Dmaven.javadoc.skip=true -f pom.xml
```

Expected: failure because `OpenApiEnhancer` still requires `ServiceDocsScanner` and exposes
`getEnhancedOpenApiJson()`.

- [ ] **Step 3: Implement the pure adapter**

`OpenApiEnhancer` becomes:

```java
public class OpenApiEnhancer implements OpenApiEnhancerSpi {
    private final V3OpenApiEnhancer delegate;
    private final V3ServiceRegistry registry;

    @Override
    public String enhance(String rawOpenApiJson) {
        return delegate.enhance(rawOpenApiJson, registry);
    }
}
```

Remove `ServiceDocsScanner` from `ApidocAutoConfiguration#openApiEnhancer`.

- [ ] **Step 4: Run all R2 tests and commit**

Run:

```bash
mvn test -Dmaven.javadoc.skip=true -f pom.xml
```

Expected: all tests pass.

Commit:

```bash
git add -A
git commit -m "refactor: make OpenAPI enhancer stateless"
```

### Task 3: Attachment resource migration

**Files:**
- Modify: `isass-service-attachment-service/src/main/resources/smart-doc.json`
- Delete: `isass-service-attachment-service/src/main/resources/service-docs/**`
- Create: `isass-service-attachment-service/src/main/resources/openapi3/openapi.json`

- [ ] **Step 1: Verify the old configuration**

Run:

```bash
rg -n '"outPath": "src/main/resources/service-docs/api"' \
  isass-service-attachment-service/src/main/resources/smart-doc.json
```

Expected: one match.

- [ ] **Step 2: Hard-switch smart-doc output and delete old resources**

Set:

```json
"outPath": "src/main/resources/openapi3"
```

Delete the entire `src/main/resources/service-docs/` tree. Do not modify the screw profile or
its `${project.basedir}/src/main/resources/service-docs/database` output configuration.

- [ ] **Step 3: Generate and verify**

Run:

```bash
mvn -pl isass-service-attachment-service -am compile \
  -DskipTests -Dmaven.javadoc.skip=true -f pom.xml
test -f isass-service-attachment-service/src/main/resources/openapi3/openapi.json
test ! -e isass-service-attachment-service/src/main/resources/service-docs
```

Expected: build success, new file exists, old tree does not exist.

- [ ] **Step 4: Commit only scoped R3 changes**

Preserve the user's pre-existing root/service POM edits. Stage only the smart-doc config,
deleted service-doc resources, and new `openapi3/openapi.json`:

```bash
git add isass-service-attachment-service/src/main/resources/smart-doc.json \
  isass-service-attachment-service/src/main/resources/service-docs \
  isass-service-attachment-service/src/main/resources/openapi3/openapi.json
git commit -m "refactor: move generated OpenAPI to openapi3 resource"
```

### Task 4: Documentation and cross-repository verification

**Files:**
- Modify: `docs/usage/apidoc/smart-doc.md`
- Modify: `README.md`
- Modify: current non-historical design/roadmap statements that still prescribe the old path

- [ ] **Step 1: Update current documentation**

Describe `openapi3/openapi.json` as the only generated API document path. Remove current
instructions that advertise zyplayer or `/service-docs`. Do not rewrite dated historical plans.
State explicitly that screw remains opt-in and may recreate `service-docs/database`.

- [ ] **Step 2: Scan for active old references**

Run:

```bash
rg -n "service-docs/api|ServiceDocsScanner|GET .*/service-docs" \
  README.md docs/usage docs/design isass-web-springmvc/src
```

Expected: no active implementation/usage references; historical zyplayer design documents may
remain only when clearly historical.

- [ ] **Step 3: Install in dependency order**

Run R1, R2, R3 root installs in that order:

```bash
mvn install -DskipTests -Dmaven.javadoc.skip=true -f pom.xml
```

Expected: all three builds succeed.

- [ ] **Step 4: Final audit**

Verify R1 and R2 are clean after commits. Verify R3 contains only the user's pre-existing POM
changes after the scoped resource commit. Record final HEADs and test counts.

