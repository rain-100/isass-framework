# Single Nocode Migration Implementation Plan

**Goal:** Replace the parallel legacy, V2 and 零代码 nocode implementations with one unversioned nocode architecture, using attachment as the business-service migration sample and apidoc as the V4 documentation-service migration sample.

**Architecture:** The current 零代码 dynamic contract, HTTP, gRPC, OpenAPI and generator implementation becomes the sole nocode implementation and is renamed to unversioned packages and types. Attachment migrates in the same change stream; auth and im remain untouched on their 零代码 branches until their own upgrade work begins.

## Global constraints

- Do not modify auth or im repositories/branches.
- Include `isass-service-apidoc`; migrate its OpenAPI enhancement and service-side nocode contract references with the framework.
- Preserve the current 零代码 runtime behaviour while names are migrated.
- Every framework change updates `docs/60.changelog/ChangeLog4.x.md` and ends with root `mvn install`.
- Do not leave compatibility wrappers after the final deletion stage.
- Do not restart or terminate IDEA-managed services.

### Task 1: Establish the unversioned nocode namespace

- Move `vip.isass.framework.nocode` packages to `vip.isass.framework.nocode` subpackages.
- Rename public 零代码 types to unversioned names, beginning with contracts, services, streams, transports and table metadata.
- Update all framework imports and tests; prove core, HTTP and gRPC tests pass.

### Task 2: Migrate the generator and generated source model

- Rename generator classes, Maven goal metadata, templates and generated contract resource names to unversioned names.
- Make generated API/service/entity/criteria/repository names unversioned.
- Update attachment generator invocation and regenerate its contract/source sample.

### Task 3: Migrate attachment as the V4 sample service

- Rename its 零代码 entities, criteria, repositories and services to final names.
- Replace all `IV3*` API contracts and 零代码 imports with the unversioned framework API.
- Compile attachment and verify generated OpenAPI and file endpoints remain present.

### Task 4: Migrate apidoc as the V4 documentation-service sample

- Replace all 零代码 contract, type and OpenAPI enhancer imports with the unversioned nocode API.
- Rename 零代码-specific schema/component conventions only where they are framework implementation details; preserve published OpenAPI paths and document behaviour.
- Compile apidoc and verify the enhanced OpenAPI document and Knife4j page still load.

### Task 5: Delete V2 nocode and the prior unmarked nocode implementation

- Remove V2 controllers, services, repositories, criteria, templates, converter and response-advice branches.
- Remove obsolete pre-versioned nocode classes after confirming no framework or attachment reference remains.
- Remove obsolete auto-configurations and documentation references.

### Task 6: Delete 零代码 compatibility names and complete verification

- Delete every `nocode` package and every 零代码 compatibility type, template and contract resource.
- Run full framework install, attachment and apidoc builds, plus targeted HTTP/gRPC/OpenAPI/generator tests.
- Rewrite framework and attachment documentation to describe only the final unversioned architecture.
