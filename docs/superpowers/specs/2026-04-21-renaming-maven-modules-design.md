# Design: Maven Module Renaming

**Goal:** Rename all Maven modules in the project from the `super` and `kernel-net` naming scheme to the `isass-framework` naming scheme, referencing the naming conventions found in `isass-framework-v4-old`. The directory structure will remain unchanged.

## Mapping Table

| Current Folder | Current artifactId | New artifactId |
|---|---|---|
| (root) | super | isass-framework |
| kernel-net-admin | kernel-net-admin | isass-framework-net-admin |
| kernel-net-core | kernel-net-core | isass-framework-net-core |
| kernel-net-netty | kernel-net-netty | isass-framework-net-netty |
| kernel-net-proxy-core | kernel-net-proxy-core | isass-framework-net-proxy-core |
| kernel-net-proxy-service | kernel-net-proxy-service | isass-framework-net-proxy-server |
| kernel-net-proxy-upstream | kernel-net-proxy-upstream | isass-framework-net-proxy-upstream |
| kernel-net-socketio | kernel-net-socketio | isass-framework-net-socketio |
| kernel-net-websocket | kernel-net-websocket | isass-framework-net-websocket |
| super-core | super-core | isass-framework-common |
| super-core-cache | super-core-cache | isass-framework-database-redis |
| super-core-database | super-core-database | isass-framework-database-core |
| super-core-database-mybatisplus | super-core-database-mybatisplus | isass-framework-database-mybatisplus |
| super-core-database-mybatisplus-mysql | super-core-database-mybatisplus-mysql | isass-framework-database-mybatisplus-mysql |
| super-core-database-mybatisplus-postgresql | super-core-database-mybatisplus-postgresql | isass-framework-database-mybatisplus-postgresql |
| super-core-database-mysql | super-core-database-mysql | isass-framework-database-mysql |
| super-core-database-postgresql | super-core-database-postgresql | isass-framework-database-postgresql |
| super-core-database-querydsl | super-core-database-querydsl | isass-framework-database-querydsl |
| super-core-deploy | super-core-deploy | isass-framework-deploy |
| super-core-elasticsearch | super-core-elasticsearch | isass-framework-database-elasticsearch |
| super-core-encryption | super-core-encryption | isass-framework-encryption |
| super-core-mq | super-core-mq | isass-framework-mq-core |
| super-core-mq-kafka011 | super-core-mq-kafka011 | isass-framework-mq-kafka011 |
| super-core-mq-ons | super-core-mq-ons | isass-framework-mq-ons |
| super-core-mq-spring-event | super-core-mq-spring-event | isass-framework-mq-springevent |
| super-core-protobuf | super-core-protobuf | isass-framework-serialization-protobuf |
| super-core-web | super-core-web | isass-framework-web-springmvc |
| super-core-web-security | super-core-web-security | isass-framework-security-springsecurity |
| super-core-web-swagger | super-core-web-swagger | isass-framework-web-swagger |

## Renaming Strategy

1.  **Bulk artifactId Update:** Update the `<artifactId>` in the parent `pom.xml` and all module `pom.xml` files.
2.  **Update Parent References:** Update the `<parent><artifactId>` in all module `pom.xml` files to point to the new parent artifactId.
3.  **Update Inter-module Dependencies:** Update all `<dependency>` entries that point to other modules within the same project.
4.  **Update Dependency Management:** Update the `<dependencyManagement>` section in the root `pom.xml`.
5.  **No Directory Renaming:** Keep the existing folder names as they are.

## Verification

-   Ensure the project still loads correctly (e.g., via `mvn help:effective-pom`).
-   Verify all modules use the new names by checking the effective artifactIds.
