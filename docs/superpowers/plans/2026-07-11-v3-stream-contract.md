# V3 Stream Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let IV3 service contracts automatically expose ordinary HTTP/gRPC calls and streaming upload/download calls without business MVC controllers.

**Architecture:** Contract generation makes custom `@http` mandatory. The HTTP adapter returns `Resp` only for ordinary values and maps stream contracts to raw HTTP bodies. The gRPC adapter adds client/server streaming for `InputStream` and `V3FileStream`. Attachment APIs become transport-neutral and Controller code is removed when no HTTP-only endpoint remains.

**Tech Stack:** Spring MVC, Jackson, gRPC Java dynamic descriptors, Smart-Doc, JUnit 5.

## Global Constraints

- IV3 contract methods must not expose Spring MVC types.
- Custom IV3 methods require an explicit `@http METHOD /path` Javadoc tag.
- `InputStream` is streamed; file payloads are never accumulated with `readAllBytes()`.
- Ordinary results use `Resp`; streams are raw transport responses.

---

### Task 1: Contract validation and response mode

**Files:** `isass-nocode-generator/.../V3ContractGenerator.java`, `isass-nocode-core/.../contract/*`, generator tests.

- [ ] Add failing tests for missing custom `@http` and stream return metadata.
- [ ] Add stream payload metadata to operation contracts and reject missing `@http`.
- [ ] Run generator tests.

### Task 2: Dynamic HTTP stream adapter

**Files:** `isass-nocode-http/.../V3HttpServerAdapter.java`, client transport, HTTP tests.

- [ ] Add failing tests for ordinary `Resp` wrapping and raw file-stream responses.
- [ ] Bind multipart parts to `InputStream` without byte-array conversion.
- [ ] Return raw streaming HTTP responses for `V3FileStream`; preserve `Resp` rules for ordinary methods.
- [ ] Run HTTP tests.

### Task 3: Dynamic gRPC stream adapter

**Files:** `isass-nocode-grpc/...`, generated proto writer, gRPC tests.

- [ ] Add failing tests for client-stream upload and server-stream download descriptors.
- [ ] Add chunk messages and streaming descriptors; do not use `readAllBytes()` for stream operations.
- [ ] Reconstruct contract input/output streams at the remote proxy boundary.
- [ ] Run gRPC tests.

### Task 4: Attachment contract migration

**Files:** attachment API/service/controller and tests.

- [ ] Replace `MultipartFile`/`ResponseEntity<Resource>` contract types with `InputStream`/`V3FileStream`.
- [ ] Move ordinary attachment and icon-group business methods into IV3 interfaces with `@http`.
- [ ] Delete migrated V3 business controllers; retain only FileSystem HTTP-only endpoints.
- [ ] Regenerate OpenAPI and run attachment tests.

### Task 5: Documentation and full verification

**Files:** V3 contract/transport documentation.

- [ ] Document `@http`, stream method signatures, and response wrapping.
- [ ] Run framework and attachment test suites.
