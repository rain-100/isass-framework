# Spring Boot Adapter Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the Spring Boot integration entry point for common framework components into `isass-adapter-springboot`.

**Architecture:** `isass-core-common` keeps common code, while `isass-adapter-springboot` becomes the Boot auto-configuration module that scans `vip.isass.framework.common`. Attachment service depends on the adapter to prove the migration path.

`isass-adapter-springboot` is limited to core/common Spring Boot integration. Feature modules keep their own Spring auto-configuration so business services only receive the database, mq, net, web, or security features they explicitly depend on.

**Tech Stack:** Java 25, Maven 4, Spring Boot auto-configuration, JUnit 5, AssertJ.

---

### Task 1: Roadmap Adjustment

**Files:**
- Modify: `isass-framework-v4/docs/70.roadmap/roadmap.md`

- [x] Mark the CTE recursive-query item as postponed and outside the current implementation scope.

### Task 2: Adapter Auto-Configuration

**Files:**
- Create: `isass-framework-v4/isass-adapter-springboot/src/test/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfigurationTest.java`
- Create: `isass-framework-v4/isass-adapter-springboot/src/main/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfiguration.java`
- Create: `isass-framework-v4/isass-adapter-springboot/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `isass-framework-v4/isass-adapter-springboot/pom.xml`
- Modify: `isass-framework-v4/isass-core-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [x] Write a test that loads the adapter auto-configuration and expects the bean provider facade to be initialized.
- [x] Implement the adapter auto-configuration and imports file.
- [x] Remove the core-common auto-configuration import so common no longer auto-registers itself.

### Task 3: Dependency Wiring

**Files:**
- Modify: `isass-framework-v4/isass-core-dependencies/pom.xml`
- Modify: `isass-service-attachment/isass-service-attachment-service/pom.xml`

- [x] Add `isass-adapter-springboot` to framework dependency management.
- [x] Add `isass-adapter-springboot` to attachment service dependencies.

### Task 4: Verification

**Commands:**
- `mvn -pl isass-adapter-springboot test`
- `mvn -pl isass-adapter-springboot,isass-core-common install -DskipTests`
- In attachment service: run a compile/test command that does not require unavailable infrastructure.

- [x] Run green verification only and report exact outcomes.

### Task 5: Adapter Boundary And Web Exception Split

**Files:**
- Create: `isass-framework-v4/docs/design/springboot-adapter-boundary.md`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/test/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfigurationTest.java`
- Modify: `isass-framework-v4/isass-core-common/pom.xml`
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/exception/BuildInCoreExceptionMapping.java`
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/support/okhttp/OkHttpUtil.java`
- Modify: `isass-framework-v4/isass-web-springmvc/src/main/java/vip/isass/framework/web/exception/BuildInWebExceptionMapping.java`
- Create: `isass-framework-v4/isass-web-springmvc/src/test/java/vip/isass/framework/web/exception/BuildInWebExceptionMappingTest.java`

- [x] Document that feature module Spring auto-configurations stay in their own modules.
- [x] Add a guard test that adapter does not bring database, mq, net, web, or security auto-configurations.
- [x] Move Spring MVC exception mappings from core-common to web-springmvc.
- [x] Remove direct Spring WebMVC usage from core-common code.

### Task 6: Security Spring Component Split

**Files:**
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/web/security/authentication/ms/MsAuthenticationHeaderProvider.java`
- Create: `isass-framework-v4/isass-security-springsecurity/src/main/java/vip/isass/framework/web/security/authentication/ms/SpringMsAuthenticationHeaderProvider.java`
- Create: `isass-framework-v4/isass-security-springsecurity/src/test/java/vip/isass/framework/web/security/authentication/ms/SpringMsAuthenticationHeaderProviderTest.java`
- Modify: `isass-framework-v4/isass-security-springsecurity/pom.xml`

- [x] Keep the existing core class as a pure Java request header provider and constants holder.
- [x] Add the Spring `@Component` provider in `isass-security-springsecurity`.
- [x] Verify security module registers the provider as both `MsAuthenticationHeaderProvider` and `AdditionalRequestHeaderProvider`.

### Task 7: Security Role Code Manager Split（已被 Entrypoint 重构取代）

- [x] 删除旧角色编码服务、聚合 Manager 和 Spring/BSP 适配器链。
- [x] `DefaultSecurityMetadataSourceProvider` 直接依赖 Security 模块的 `IAuthorizationService`。
- [x] BSP 使用本地授权实现，其他微服务由 Entrypoint registry 注入远程代理。

### Task 8: Common Select Option Manager Split

**Files:**
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/selectoption/SelectOptionServiceManager.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/main/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfiguration.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/test/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfigurationTest.java`

- [x] Keep the core select-option manager as a pure Java aggregator.
- [x] Register the manager from `isass-adapter-springboot` using Spring `ObjectProvider`.
- [x] Verify empty and populated Spring registration cases.

### Task 9: Auto Destroy Manager Adapter Move

**Files:**
- Delete: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/spring/bean/destroy/AutoDestroyManager.java`
- Create: `isass-framework-v4/isass-adapter-springboot/src/main/java/vip/isass/framework/adapter/springboot/destroy/AutoDestroyManager.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/main/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfiguration.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/test/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfigurationTest.java`

- [x] Keep `AutoDestroyable` in core as a marker interface.
- [x] Move the Spring `ApplicationReadyEvent` listener into `isass-adapter-springboot`.
- [x] Verify adapter auto-configuration registers the auto-destroy manager.

### Task 10: DB Entity Converter Configuration Split

**Files:**
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/entity/DbEntityConvert.java`
- Modify: `isass-framework-v4/isass-core-common/src/main/java/vip/isass/framework/common/structure/entity/V2DbEntityConvert.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/main/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfiguration.java`
- Modify: `isass-framework-v4/isass-adapter-springboot/src/test/java/vip/isass/framework/adapter/springboot/IsassSpringBootAutoConfigurationTest.java`

- [x] Remove Spring `@Component`, `@Value`, and `@Resource` usage from the core converter classes.
- [x] Register converter beans from `isass-adapter-springboot` and inject `info.package`.
- [x] Verify the configured package is applied to both converter classes.
