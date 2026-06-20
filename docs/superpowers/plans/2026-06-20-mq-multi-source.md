# MQ Multi Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the v3-style MQ manufacturer model with a breaking v4 multi-source MQ abstraction.

**Architecture:** `isass-mq-core` owns source configuration, factories, producers, consumers, and publishing. Concrete modules provide `IMqFactory` implementations for Spring Event and Kafka 0.11.

**Tech Stack:** Java 25, Maven 4, Spring Boot 4 auto-configuration imports, JUnit/Surefire, Lombok, Kafka client 0.11.

---

### Task 1: Core Multi-Source API

**Files:**
- Modify: `isass-mq-core/src/main/java/vip/isass/framework/mq/core/**`
- Create tests under: `isass-mq-core/src/test/java/vip/isass/framework/mq/core/**`

- [ ] Add failing tests for primary routing, explicit source routing, disabled source filtering, and missing source failure.
- [ ] Add `DynamicMqProperties`, `MqSourceProperties`, `IMqFactory`, `IMqProducer`, `IMqConsumer`, `IMqMessageHandler`, and `MqManager`.
- [ ] Replace `EventPublisher` with `MqPublisher`.
- [ ] Run `mvn -pl isass-mq-core -am test -DskipJavadoc`.

### Task 2: Spring Event Source

**Files:**
- Modify: `isass-mq-springevent/src/main/java/vip/isass/framework/mq/spring/event/**`
- Create tests under: `isass-mq-springevent/src/test/java/vip/isass/framework/mq/spring/event/**`

- [ ] Add failing tests for source-scoped handler dispatch.
- [ ] Implement `SpringEventMqFactory`, producer, consumer, and source properties.
- [ ] Run `mvn -pl isass-mq-springevent -am test -DskipJavadoc`.

### Task 3: Kafka 0.11 Source

**Files:**
- Modify: `isass-mq-kafka011/src/main/java/vip/isass/framework/mq/kafka011/**`
- Create tests under: `isass-mq-kafka011/src/test/java/vip/isass/framework/mq/kafka011/**`

- [ ] Add failing tests for factory wiring and producer property construction.
- [ ] Adapt Kafka producer and consumer to `MqSourceProperties`.
- [ ] Run `mvn -pl isass-mq-kafka011 -am test -DskipJavadoc`.

### Task 4: Documentation and Verification

**Files:**
- Modify: `isass-mq-core/readme.md`
- Modify: `isass-mq-springevent/readme.md`
- Modify: `isass-mq-kafka011/readme.md`
- Modify: `docs/60.changelog/ChangeLog4.x.md`

- [ ] Update examples to use `isass.mq.sources`.
- [ ] Run `mvn -pl isass-mq-core,isass-mq-springevent,isass-mq-kafka011 -am test -DskipJavadoc`.
- [ ] Run repository-level `mvn install` when the module build passes.
