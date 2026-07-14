# Nocode Unified Service Proxy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a V4 `IService` API interface resolve to its local Spring implementation when available, otherwise invoke the matching remote nocode service through gRPC and then HTTP.

**Architecture:** Core discovers every classpath `nocode-contract.json` and registers a typed proxy only for interfaces without a local Spring implementation. The proxy uses the existing `ServiceProxyFactory` and an ordered set of remote transports. HTTP is a single dynamic Spring `@HttpExchange` client; gRPC reuses the existing dynamic descriptors and receives channels from endpoint configuration.

**Tech Stack:** Java 25, Spring Boot 4 / Spring Framework 7 `@HttpExchange`, Spring `RestClientAdapter`, gRPC 1.80, Jackson, JUnit 5.

## Global Constraints

- Only V4 contracts in `META-INF/isass/nocode-contract.json` participate; historic V1/V2 package names never participate.
- Local implementation has priority by not registering a competing proxy Bean.
- Remote priority is gRPC then HTTP; only idempotent operations can fall through after a sent-request transport failure.
- HTTP must use one dynamic `@HttpExchange` interface, not generated per-service or per-verb clients.
- All configuration keys start with `isass.framework.nocode`.
- Errors must name service, entity, interface, and operation; no silent fallback.

---

### Task 1: Core contract-to-interface discovery and proxy registration

**Files:**
- Create: `isass-nocode-core/src/main/java/vip/isass/framework/nocode/transport/RemoteTransportProvider.java`
- Create: `isass-nocode-core/src/main/java/vip/isass/framework/nocode/transport/ServiceProxyRegistrar.java`
- Modify: `isass-nocode-core/src/main/java/vip/isass/framework/nocode/AutoConfiguration.java`
- Modify: `isass-nocode-core/src/main/java/vip/isass/framework/nocode/transport/ServiceProxyFactory.java`
- Test: `isass-nocode-core/src/test/java/vip/isass/framework/nocode/transport/ServiceProxyRegistrarTest.java`

**Interfaces:**

```java
public interface RemoteTransportProvider {
    List<InvocationTransport> transports(ServiceContract contract);
}

public final class ServiceProxyRegistrar implements BeanDefinitionRegistryPostProcessor {
    // Load contracts, skip a locally implemented interface, otherwise register a FactoryBean.
}
```

- [ ] **Step 1: Write the failing registrar tests**

Create a test fixture `ExampleService extends IService<ExampleEntity, ExampleCriteria>` and contract resource. Assert: a local `ExampleService` bean results in exactly one local bean; no local bean results in one typed JDK proxy; a nonexistent `serviceInterface` fails with the resource URL and class name.

- [ ] **Step 2: Run the core test and verify failure**

Run: `mvn -pl isass-nocode-core -Dtest=ServiceProxyRegistrarTest test`

Expected: compilation failure because `ServiceProxyRegistrar` does not exist.

- [ ] **Step 3: Implement the SPI and registrar**

Use `ContractResourceLoader` with the bean factory class loader. Load `contract.serviceInterface()` with `Class.forName`; require `type.isInterface()` and `IService.class.isAssignableFrom(type)`. Detect a local implementation using `ListableBeanFactory.getBeanNamesForType(type, true, false)`. Register no proxy if any local bean exists. Otherwise register one factory Bean whose declared object type is the interface and whose invocation obtains all `RemoteTransportProvider` beans, concatenates their transports for that contract, then calls `ServiceProxyFactory.create`.

- [ ] **Step 4: Make proxy invocation dynamically read transport providers**

Add an overload to `ServiceProxyFactory.create` that accepts `Supplier<List<InvocationTransport>>`; retain the existing list overload for unit compatibility. Each interface method invocation obtains the current transport list and delegates to `TransportResolver`.

- [ ] **Step 5: Run tests and commit**

Run: `mvn -pl isass-nocode-core -Dtest=ServiceProxyRegistrarTest,ServiceProxyFactoryTest,TransportResolverTest test`

Expected: PASS.

Commit: `git commit -m "feat: register remote nocode service proxies"`

### Task 2: Dynamic `@HttpExchange` transport

**Files:**
- Create: `isass-nocode-http/src/main/java/vip/isass/framework/nocode/http/NocodeHttpExchange.java`
- Create: `isass-nocode-http/src/main/java/vip/isass/framework/nocode/http/HttpEndpointProperties.java`
- Create: `isass-nocode-http/src/main/java/vip/isass/framework/nocode/http/HttpRemoteTransportProvider.java`
- Modify: `isass-nocode-http/src/main/java/vip/isass/framework/nocode/http/HttpClientTransport.java`
- Modify: `isass-nocode-http/src/main/java/vip/isass/framework/nocode/http/HttpAutoConfiguration.java`
- Test: `isass-nocode-http/src/test/java/vip/isass/framework/nocode/http/HttpClientTransportTest.java`

**Interfaces:**

```java
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
interface NocodeHttpExchange {
    JsonNode exchange(HttpMethod method, URI uri,
        @RequestParam MultiValueMap<String, String> query,
        @RequestBody(required = false) Object body);
}

@ConfigurationProperties("isass.framework.nocode.http")
public class HttpEndpointProperties {
    private Map<String, URI> endpoints = new LinkedHashMap<>();
}
```

- [ ] **Step 1: Write failing MockWebServer tests**

Test a GET operation with path and repeated query values, and a POST body operation. Verify method, full URI, JSON body, `Accept: application/json`, and conversion of `{"data": ...}` to the contract return type. Test no configured endpoint reports the service name.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -pl isass-nocode-http -Dtest=HttpClientTransportTest test`

Expected: compilation failure because `NocodeHttpExchange` and endpoint properties do not exist.

- [ ] **Step 3: Implement a single dynamic exchange client**

Create `RestClient`, adapt it with `RestClientAdapter.create`, then create exactly one `NocodeHttpExchange` using `HttpServiceProxyFactory.builderFor(...).build().createClient(...)`. `HttpClientTransport` converts `OperationContract` variables and query fields to `URI` plus `MultiValueMap`, calls `exchange(HttpMethod, URI, query, body)`, and converts response `data` by the declared canonical Java type.

- [ ] **Step 4: Wire HTTP provider and configuration**

Enable `HttpEndpointProperties` in `HttpAutoConfiguration`. `HttpRemoteTransportProvider` returns the transport only when `endpoints` includes `contract.serviceName()`. Preserve the server adapter beans.

- [ ] **Step 5: Run tests and commit**

Run: `mvn -pl isass-nocode-http -am -Dtest=HttpClientTransportTest,HttpServerAdapterTest test`

Expected: PASS.

Commit: `git commit -m "feat: add dynamic httpexchange nocode client"`

### Task 3: Configured dynamic gRPC client transport

**Files:**
- Create: `isass-nocode-grpc/src/main/java/vip/isass/framework/nocode/grpc/GrpcEndpointProperties.java`
- Create: `isass-nocode-grpc/src/main/java/vip/isass/framework/nocode/grpc/GrpcRemoteTransportProvider.java`
- Modify: `isass-nocode-grpc/src/main/java/vip/isass/framework/nocode/grpc/GrpcAutoConfiguration.java`
- Test: `isass-nocode-grpc/src/test/java/vip/isass/framework/nocode/grpc/GrpcRemoteTransportProviderTest.java`

**Interfaces:**

```java
@ConfigurationProperties("isass.framework.nocode.grpc")
public class GrpcEndpointProperties {
    private Map<String, String> endpoints = new LinkedHashMap<>();
}
```

- [ ] **Step 1: Write failing in-process gRPC provider test**

Start the existing `GrpcServerAdapter` with an in-process server. Configure the contract service name to the in-process target. Assert `GrpcRemoteTransportProvider` supplies an available `GrpcClientTransport`, invokes the logical operation, and returns the decoded value. Assert a service with no endpoint has no gRPC transport.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -pl isass-nocode-grpc -Dtest=GrpcRemoteTransportProviderTest test`

Expected: compilation failure because the provider and properties are absent.

- [ ] **Step 3: Implement provider lifecycle**

Create `ManagedChannel` per configured target lazily, use it to create `GrpcClientTransport`, and close every created channel on bean destruction. Do not expose a channel for missing endpoint. Preserve `GrpcClientTransport.available`: streaming download remains supported; upload `InputStream` remains unavailable so resolver can use HTTP.

- [ ] **Step 4: Wire gRPC provider and configuration**

Enable `GrpcEndpointProperties` and register `GrpcRemoteTransportProvider` through `GrpcAutoConfiguration`; preserve server-side `GrpcServerAdapter` beans.

- [ ] **Step 5: Run tests and commit**

Run: `mvn -pl isass-nocode-grpc -am -Dtest=GrpcRemoteTransportProviderTest,GrpcServerAdapterTest test`

Expected: PASS.

Commit: `git commit -m "feat: add configured nocode grpc client"`

### Task 4: End-to-end precedence, diagnostics, and documentation

**Files:**
- Test: `isass-nocode-core/src/test/java/vip/isass/framework/nocode/transport/ServiceProxyRegistrarTest.java`
- Modify: `isass-nocode-core/src/main/java/vip/isass/framework/nocode/transport/TransportResolver.java`
- Modify: `docs/usage/nocode/frontend-api-usage.md`
- Modify: `docs/superpowers/specs/2026-07-14-nocode-service-proxy-design.md`

- [ ] **Step 1: Write precedence and failure tests**

Assert: local interface has no proxy; gRPC is selected before HTTP; an idempotent gRPC `TransportInvocationException(requestSent=true)` retries HTTP; a non-idempotent sent request rethrows gRPC error; empty transports throw `TransportUnavailableException` including logical service/entity/operation.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -pl isass-nocode-core -Dtest=ServiceProxyRegistrarTest,TransportResolverTest test`

Expected: at least the provider-backed precedence tests fail before the resolver diagnostic update.

- [ ] **Step 3: Complete diagnostics and docs**

Ensure every startup error includes the source contract resource plus interface name; ensure every call failure includes `serviceName/entityName/operationName`. Document the two endpoint maps, local-priority behavior, no V1/V2 compatibility, and automatic HTTP/gRPC endpoint selection.

- [ ] **Step 4: Run complete framework validation and commit**

Run: `mvn -pl isass-nocode-core,isass-nocode-http,isass-nocode-grpc -am test`

Expected: PASS.

Commit: `git commit -m "docs: document nocode remote service clients"`
