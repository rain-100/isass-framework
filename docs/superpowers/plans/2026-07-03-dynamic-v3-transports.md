# Dynamic V3 Transports Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace generated V3 Controllers with contract-driven dynamic HTTP and gRPC adapters while keeping `IV3XxxService` as the only local and remote Java API.

**Architecture:** `isass-nocode-core` owns immutable operation descriptors and resolution APIs. `isass-nocode-generator` emits contract/proto artifacts from service interfaces and Javadoc. `isass-nocode-http` and `isass-nocode-grpc` expose those descriptors through one adapter Bean each and provide remote transports selected local-first, then gRPC, then HTTP.

**Tech Stack:** Java 25, Maven 4, Spring MVC 7, Jackson 3, gRPC Java, protobuf, OpenTelemetry, JUnit 5, AssertJ.

---

### Task 1: Introduce the transport-independent contract model

**Files:**
- Create contract classes under `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v3/contract/`
- Create resolver classes under `isass-nocode-core/src/main/java/vip/isass/framework/nocode/v3/transport/`
- Test under `isass-nocode-core/src/test/java/vip/isass/framework/nocode/v3/contract/`

- [ ] Write failing serialization, route-validation and method-overload tests.
- [ ] Implement service, operation, parameter, type and HTTP descriptors.
- [ ] Implement resource loading and content-hash validation.
- [ ] Implement local/gRPC/HTTP selection and safe fallback policy abstractions.
- [ ] Run `mvn -pl isass-nocode-core test`.

### Task 2: Create the build-time contract generator

**Files:**
- Create module `isass-nocode-generator/`
- Modify root and dependency-management POMs.
- Create Maven plugin/generator tests with source fixtures.

- [ ] Write failing tests for standard operations, custom `@http`, Javadoc, order, multi-parameter requests and invalid overloads.
- [ ] Parse `IV3XxxService`, entity/criteria/DTO source and Javadoc.
- [ ] Generate `META-INF/isass/v3-contract.json`.
- [ ] Generate protobuf files with one service/method per logical operation.
- [ ] Integrate protoc generated sources without creating Spring Beans.
- [ ] Run generator tests and verify deterministic output.

### Task 3: Add the dynamic HTTP adapter

**Files:**
- Create module `isass-nocode-http/`
- Create dynamic handler mapping, invocation adapter, binders and remote client.
- Add MockMvc integration tests.

- [ ] Write failing tests proving one adapter serves two entities and a custom `@http` method.
- [ ] Implement standard route dispatch and custom route template matching.
- [ ] Implement path/query/body binding and `Resp` wrapping.
- [ ] Implement HTTP remote invocation transport.
- [ ] Verify no entity Controller Bean is required.

### Task 4: Generate V3 OpenAPI from contracts

**Files:**
- Modify `isass-service-apidoc` V3 OpenAPI integration.
- Add contract-based OpenAPI tests.

- [ ] Replace typed-Controller path folding with contract projection.
- [ ] Preserve named schemas and Javadoc descriptions.
- [ ] Preserve operation naming, `v3_` IDs, `@order`/`x-order`, criteria filtering and oneOf mappings.
- [ ] Leave ordinary Smart-Doc Controller operations unchanged.
- [ ] Run the full apidoc suite.

### Task 5: Add the dynamic gRPC adapter

**Files:**
- Create module `isass-nocode-grpc/`
- Add dynamic service-definition, marshaller, client transport and observability classes.
- Add in-process gRPC tests.

- [ ] Write failing tests for distinct logical `MethodDescriptor` names with one adapter Bean.
- [ ] Register generated protobuf methods through dynamic `ServerServiceDefinition`.
- [ ] Invoke local `IV3Service` implementations through contract descriptors.
- [ ] Implement gRPC remote transport.
- [ ] Record native method telemetry and bounded Isass attributes.

### Task 6: Implement service proxies and safe transport resolution

**Files:**
- Modify core transport resolver and Spring adapter auto-configurations.
- Add resolver and proxy integration tests.

- [ ] Verify local implementation wins.
- [ ] Verify gRPC wins over HTTP when both are advertised.
- [ ] Verify HTTP is selected when gRPC is absent or its circuit is open.
- [ ] Verify a sent mutating gRPC request never retries over HTTP.
- [ ] Verify explicitly idempotent queries may opt into fallback.

### Task 7: Migrate attachment and remove generated Controllers

**Files:**
- Modify attachment API/service POMs and generator invocation.
- Delete generated `V3IconController` and `V3IconGroupController`.
- Generate attachment contract/proto/OpenAPI resources.

- [ ] Add a custom `IV3XxxService` fixture method with `@http`.
- [ ] Verify standard and custom HTTP operations.
- [ ] Verify local service resolution.
- [ ] Verify OpenAPI/Knife4j output.
- [ ] Verify in-process gRPC invocation and logical method telemetry.

### Task 8: Publish final architecture and verify

**Files:**
- Rewrite framework V3 design/usage docs as final-state documentation.

- [ ] Remove Controller-generation descriptions and obsolete artifacts.
- [ ] Document contract tags, binding, module ownership, transport priority and safe fallback.
- [ ] Run all affected Maven tests, frontend build and Smart-Doc generation.
- [ ] Run `git diff --check` in every repository.
- [ ] Commit each repository independently.
