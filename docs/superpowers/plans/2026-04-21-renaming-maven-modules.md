# Renaming Maven Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the root artifactId and all internal dependency artifactIds in the root `pom.xml` to follow the new naming convention.

**Architecture:** This is a purely textual update to the root `pom.xml`. No physical directories are moved.

**Tech Stack:** Maven (XML)

---

### Task 1: Environment Setup

**Files:**
- N/A

- [ ] **Step 1: Create and switch to the `rename-modules` branch**

Run: `git checkout -b rename-modules`
Expected: Switched to a new branch 'rename-modules'

---

### Task 2: Update Root ArtifactId

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Replace root artifactId**

Replace:
```xml
    <groupId>vip.isass</groupId>
    <artifactId>super</artifactId>
```
With:
```xml
    <groupId>vip.isass</groupId>
    <artifactId>isass-framework</artifactId>
```

---

### Task 3: Update Dependency Management Entries

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Replace all mapped artifactIds in dependencyManagement**

Perform the following replacements within the `<dependencyManagement>` section for entries with `groupId` `vip.isass`:

1. `super-core` -> `isass-framework-common`
2. `super-core-deploy` -> `isass-framework-deploy`
3. `super-core-web` -> `isass-framework-web-springmvc`
4. `super-core-web-security` -> `isass-framework-security-springsecurity`
5. `super-core-web-swagger` -> `isass-framework-web-swagger`
6. `super-core-encryption` -> `isass-framework-encryption`
7. `super-core-protobuf` -> `isass-framework-serialization-protobuf`
8. `super-core-cache` -> `isass-framework-database-redis`
9. `super-core-database` -> `isass-framework-database-core`
10. `super-core-database-mysql` -> `isass-framework-database-mysql`
11. `super-core-database-postgresql` -> `isass-framework-database-postgresql`
12. `super-core-database-mybatisplus` -> `isass-framework-database-mybatisplus`
13. `super-core-database-querydsl` -> `isass-framework-database-querydsl`
14. `super-core-database-mybatisplus-postgresql` -> `isass-framework-database-mybatisplus-postgresql`
15. `super-core-database-mybatisplus-mysql` -> `isass-framework-database-mybatisplus-mysql`
16. `super-core-mq` -> `isass-framework-mq-core`
17. `super-core-mq-ons` -> `isass-framework-mq-ons`
18. `super-core-mq-spring-event` -> `isass-framework-mq-springevent`
19. `super-core-mq-kafka011` -> `isass-framework-mq-kafka011`
20. `kernel-net-admin` -> `isass-framework-net-admin`
21. `kernel-net-core` -> `isass-framework-net-core`
22. `kernel-net-netty` -> `isass-framework-net-netty`
23. `kernel-net-websocket` -> `isass-framework-net-websocket`
24. `kernel-net-socketio` -> `isass-framework-net-socketio`
25. `kernel-net-proxy-core` -> `isass-framework-net-proxy-core`
26. `kernel-net-proxy-upstream` -> `isass-framework-net-proxy-upstream`
27. `kernel-net-proxy-service` -> `isass-framework-net-proxy-server`
28. `super-core-elasticsearch` -> `isass-framework-database-elasticsearch`

---

### Task 4: Verification

**Files:**
- N/A

- [ ] **Step 1: Run Maven effective-pom**

Run: `mvn help:effective-pom -N`
Expected: Build SUCCESS and the output shows the new artifactId `isass-framework`.

---

### Task 5: Commit Changes

**Files:**
- N/A

- [ ] **Step 1: Commit the changes**

Run: `git add pom.xml && git commit -m "chore: rename root artifactId to isass-framework"`
Expected: [rename-modules ...] chore: rename root artifactId to isass-framework
