# Dynamic V3 HTTP and gRPC Transport Design

## Objective

Use `IV3XxxService` as the only Java business contract for standard V3 operations and custom business methods. Remove generated V3 Controller Java files while retaining strongly typed local and remote calls.

Runtime transport selection is:

```text
local implementation > remote gRPC implementation > remote HTTP implementation
```

The implementation must not create one HTTP Controller Bean or one gRPC implementation Bean per entity.

## Modules

### `isass-nocode-core`

Owns the transport-independent contract:

- `IV3Service`
- generated `IV3XxxService` interfaces
- entities and criteria abstractions
- operation and parameter descriptors
- contract resource model
- service registry and resolver APIs
- transport capability and invocation abstractions

It must not depend on Spring MVC or gRPC.

### `isass-nocode-generator`

Build-time generator that parses `IV3XxxService`, entity/DTO types, generic signatures and Javadoc.

It generates:

- `META-INF/isass/v3-contract.json`
- `.proto` files
- protobuf Java sources through protoc
- Java/protobuf conversion metadata

It does not generate V3 Controller Java files.

### `isass-nocode-http`

Spring MVC adapter containing:

- one HTTP server adapter Bean
- dynamic route matching and argument binding
- OpenAPI projection from V3 contract resources
- HTTP remote client transport

### `isass-nocode-grpc`

gRPC adapter containing:

- one Spring server adapter Bean
- dynamic `ServerServiceDefinition` registration
- one real gRPC `MethodDescriptor` per `IV3XxxService` method
- gRPC remote client transport
- OpenTelemetry semantic instrumentation

Generated protobuf/stub classes are protocol types, not Spring Beans.

## Service Contract

Generated entity service interfaces extend `IV3Service` and may declare custom methods:

```java
public interface IV3IconService
        extends IV3Service<V3Icon, V3IconCriteria> {

    /**
     * 查询可用图标
     *
     * @param tenantId 租户 ID
     * @return 可用图标
     * @http GET /available/{tenantId}
     * @order 501
     */
    List<V3Icon> findAvailableIcons(Long tenantId);
}
```

Business logic remains in the local implementation. HTTP and gRPC adapters provide transport implementations, not business behavior.

Method overloading is forbidden because operation names must uniquely identify Java, HTTP, OpenAPI and gRPC operations.

The first implementation supports synchronous unary methods only.

## Javadoc HTTP Contract

Custom methods support:

```text
@http <METHOD> <PATH>
```

Example:

```text
@http GET /available/{tenantId}
```

Binding rules:

- placeholders such as `{tenantId}` bind to same-named Java parameters
- remaining parameters of GET and DELETE bind as query parameters
- one complex parameter of POST, PUT and PATCH binds as the request body
- multiple body candidates generate a synthetic request message in the contract
- an absent `@http` defaults to `POST /action/{methodName}`
- `@param`, `@return`, `@apiNote`, `@order` and type Javadoc feed OpenAPI/proto documentation

The generator fails the build for duplicate routes, missing path parameters, overloaded methods, unsupported generic types or ambiguous body binding.

## Contract Resource

Each API module publishes `META-INF/isass/v3-contract.json`.

It records:

- service interface and application service name
- entity and criteria names/types
- operation name and Java signature
- standard/custom operation flag
- HTTP method/path/order
- parameter name, source, type and required state
- return type
- idempotency
- documentation
- protobuf service, method and message names

Contract resources are immutable build artifacts and receive a content hash/version. Provider and consumer reject incompatible contracts during capability negotiation.

## HTTP Runtime

`V3HttpServerAdapter` is one Spring Bean.

It registers or handles:

- the standard 36 V3 operations
- custom operation routes from all discovered contract resources

Requests resolve:

```text
HTTP method + serviceName + entityName + relative path
    -> V3OperationDescriptor
    -> local IV3Service implementation
    -> argument conversion and invocation
    -> Resp response
```

No entity Controller class or Bean is generated.

OpenAPI generation for V3 reads contract resources. Smart-Doc remains responsible for ordinary handwritten Controllers only.

## gRPC Runtime

The generator emits real protobuf services and methods for each `IV3XxxService` contract. The service adapter does not instantiate generated `ImplBase` Beans.

One `V3GrpcServerAdapter` Bean builds and registers dynamic `ServerServiceDefinition` instances. Each contract operation maps to a real `MethodDescriptor`:

```text
vip.isass.attachment.v3.IconService/Add
vip.isass.attachment.v3.IconService/FindByCriteria
vip.isass.attachment.v3.IconService/FindAvailableIcons
```

This retains native gRPC method-level deadlines, retry policies, authorization and observability while avoiding per-entity Spring Beans.

## Java Client Resolution

Business code injects and invokes `IV3XxxService`.

A lazy proxy resolves each call:

1. use a local non-proxy implementation when available
2. otherwise use a discovered compatible gRPC endpoint
3. otherwise use a discovered compatible HTTP endpoint
4. otherwise fail with a transport-unavailable exception

The provider's local implementation always wins.

## Safe Fallback

HTTP fallback is allowed when:

- the provider does not advertise gRPC
- endpoint selection or connection preflight fails
- the gRPC circuit breaker is already open

Once a gRPC business request has been sent, a timeout, status error or business exception does not trigger an HTTP retry.

Only explicitly idempotent query operations may opt into cross-protocol retry. Add, update and delete operations default to no cross-protocol retry.

## Observability

Native gRPC instrumentation records distinct `rpc.method` values because every logical method has a real `MethodDescriptor`.

Adapters additionally record bounded attributes:

```text
isass.v3.service
isass.v3.entity
isass.v3.operation
isass.v3.transport
isass.v3.local
isass.v3.fallback_reason
```

IDs, request bodies and user-provided values are not metric labels.

HTTP and gRPC client/server spans share trace context. Metrics cover latency, count, status, selected transport, circuit state and fallback count.

## Acceptance Criteria

- attachment contains no generated `V3*Controller.java`
- standard and custom `IV3XxxService` methods are exposed over HTTP
- custom `@http` routes and Javadoc appear in OpenAPI
- one HTTP adapter Bean serves all V3 entities
- one gRPC adapter Bean registers all logical methods
- native gRPC telemetry distinguishes logical service methods
- injected `IV3XxxService` selects local, then gRPC, then HTTP
- unsafe cross-protocol retry cannot duplicate mutating calls
- ordinary Smart-Doc Controller documentation remains unchanged
