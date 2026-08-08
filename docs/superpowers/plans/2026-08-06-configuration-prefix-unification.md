# Configuration Prefix Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move framework-provided configuration to the `isass.<module>.<feature>` namespace and remove obsolete microservice-secret configuration.

**Architecture:** Rename Spring property bindings and direct property lookups without compatibility aliases. Update BSP and asset deployment configuration and documentation to use only the new keys.

**Tech Stack:** Spring Boot configuration properties, Maven, YAML.

## Global Constraints

- Do not retain deprecated property aliases.
- Remove the unused `security.ms` configuration.
- Keep business-service-owned prefixes such as `bsp-service.*` unchanged.

---

### Task 1: Rename framework configuration bindings

**Files:**
- Modify: framework property classes and direct `@Value` consumers.
- Test: affected framework module tests.

- [ ] Rename security, HTTP endpoint, web, JSON, MQ, and boot microservice property prefixes to `isass.*`.
- [ ] Remove obsolete `security.ms` configuration support.
- [ ] Run affected framework tests.

### Task 2: Migrate service configuration and documentation

**Files:**
- Modify: BSP and asset `application.yml` files.
- Modify: framework and BSP usage documentation.

- [ ] Replace old property names with the new canonical names.
- [ ] Remove all `security.ms` entries.
- [ ] Verify no old configuration keys remain in maintained code or configuration.
