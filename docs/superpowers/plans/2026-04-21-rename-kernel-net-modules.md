# Rename kernel-net modules to isass-framework-net

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename eight `kernel-net-*` modules to `isass-framework-net-*` and update their parent artifact ID to `isass-framework`.

**Architecture:** This task involves updating `pom.xml` files for eight modules and their internal dependencies. Folder names remain unchanged.

**Tech Stack:** Maven 4 (using `<subprojects>` in root `pom.xml`)

---

### Task 1: Create the 'rename-modules' branch

- [ ] **Step 1: Create and switch to the new branch**

Run: `git checkout -b rename-modules`
Expected: Switched to a new branch 'rename-modules'

---

### Task 2: Update kernel-net-core

**Files:**
- Modify: `kernel-net-core/pom.xml`

- [ ] **Step 1: Update parent artifact ID and own artifact ID**

```xml
    <parent>
        <groupId>vip.isass</groupId>
        <artifactId>isass-framework</artifactId>
    </parent>

    <!-- 本项目信息 -->
    <artifactId>isass-framework-net-core</artifactId>
```

---

### Task 3: Update kernel-net-admin

**Files:**
- Modify: `kernel-net-admin/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

```xml
    <parent>
        <groupId>vip.isass</groupId>
        <artifactId>isass-framework</artifactId>
    </parent>

    <!-- 本项目信息 -->
    <artifactId>isass-framework-net-admin</artifactId>

    <dependencies>
        <!-- ... -->
        <dependency>
            <groupId>vip.isass</groupId>
            <artifactId>isass-framework-net-core</artifactId>
        </dependency>
        <!-- ... -->
    </dependencies>
```

---

### Task 4: Update kernel-net-netty

**Files:**
- Modify: `kernel-net-netty/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-netty`.
Update `kernel-net-core` to `isass-framework-net-core`.
Update `kernel-net-admin` to `isass-framework-net-admin`.

---

### Task 5: Update kernel-net-websocket

**Files:**
- Modify: `kernel-net-websocket/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-websocket`.
Update `kernel-net-core` to `isass-framework-net-core`.
Update `kernel-net-proxy-core` to `isass-framework-net-proxy-core`.
Update `kernel-net-admin` to `isass-framework-net-admin`.

---

### Task 6: Update kernel-net-proxy-core

**Files:**
- Modify: `kernel-net-proxy-core/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-proxy-core`.
Update `kernel-net-core` to `isass-framework-net-core`.

---

### Task 7: Update kernel-net-proxy-service

**Files:**
- Modify: `kernel-net-proxy-service/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-proxy-server` (Note: `server`, not `service`).
Update `kernel-net-proxy-core` to `isass-framework-net-proxy-core`.

---

### Task 8: Update kernel-net-proxy-upstream

**Files:**
- Modify: `kernel-net-proxy-upstream/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-proxy-upstream`.
Update `kernel-net-proxy-core` to `isass-framework-net-proxy-core`.

---

### Task 9: Update kernel-net-socketio

**Files:**
- Modify: `kernel-net-socketio/pom.xml`

- [ ] **Step 1: Update parent, own artifact ID, and dependencies**

Update parent to `isass-framework`, artifactId to `isass-framework-net-socketio`.
Update `kernel-net-core` to `isass-framework-net-core`.
Update `kernel-net-proxy-core` to `isass-framework-net-proxy-core`.
Update `kernel-net-admin` to `isass-framework-net-admin`.

---

### Task 10: Commit changes

- [ ] **Step 1: Commit everything**

Run: `git add . && git commit -m "chore: rename kernel-net modules to isass-framework-net"`
Expected: Commit successful.
