# ISASS Framework V4

Java 25 共享框架，提供服务入口、NoCode、数据库、消息、网络及安全能力。模块命名统一为 `isass-分类-能力`。

## 模块目录

- [isass-core-build](isass-core-build/README.md)：构建与部署资源。
- [isass-core-common](isass-core-common/README.md)：纯 Java 公共契约与工具。
- [isass-core-dependencies](isass-core-dependencies/README.md)：依赖与插件版本管理。
- [isass-core-parent](isass-core-parent/README.md)：服务构建父 POM。
- [isass-entrypoint-core](isass-entrypoint-core/README.md)：服务入口契约。
- [isass-entrypoint-registry](isass-entrypoint-registry/README.md)：运行时入口目录与客户端路由。
- [isass-entrypoint-http](isass-entrypoint-http/README.md)：HTTP 服务客户端。
- [isass-entrypoint-grpc](isass-entrypoint-grpc/README.md)：动态 gRPC 契约预研。
- [isass-nocode-core](isass-nocode-core/README.md)：标准 CRUD 与统一执行。
- [isass-nocode-generator](isass-nocode-generator/README.md)：模型与持久化代码生成。
- [isass-database-core](isass-database-core/README.md)：数据库与 Repository 契约。
- [isass-database-mybatisplus](isass-database-mybatisplus/README.md)：MyBatis-Plus / MPJ 适配。
- [isass-database-dameng](isass-database-dameng/README.md)：达梦数据库适配。
- [isass-database-elasticsearch](isass-database-elasticsearch/README.md)：Elasticsearch 依赖与扩展边界。
- [isass-database-redis](isass-database-redis/README.md)：Redis 支持。
- [isass-mq-core](isass-mq-core/README.md)：多源消息契约。
- [isass-mq-kafka011](isass-mq-kafka011/README.md)：Kafka 消息源。
- [isass-mq-springevent](isass-mq-springevent/README.md)：进程内事件消息源。
- [isass-mq-redisstream](isass-mq-redisstream/README.md)：Redis Stream 消息源。
- [isass-mq-redispubsub](isass-mq-redispubsub/README.md)：Redis Pub/Sub 消息源。
- [isass-net-core](isass-net-core/README.md)：网络会话与消息公共能力。
- [isass-net-admin](isass-net-admin/README.md)：网络管理接口。
- [isass-net-netty](isass-net-netty/README.md)：Netty 网络支持。
- [isass-net-proxy-core](isass-net-proxy-core/README.md)：网络代理公共能力。
- [isass-net-proxy-server](isass-net-proxy-server/README.md)：网络代理服务端。
- [isass-net-proxy-upstream](isass-net-proxy-upstream/README.md)：网络代理上游接入。
- [isass-net-socketio](isass-net-socketio/README.md)：Socket.IO 协议实现。
- [isass-net-websocket](isass-net-websocket/README.md)：WebSocket 协议实现。
- [isass-security-springsecurity](isass-security-springsecurity/README.md)：Spring Security 集成。
- [isass-encryption](isass-encryption/README.md)：文本加解密工具。
- [isass-serialization-protobuf](isass-serialization-protobuf/README.md)：Protobuf 序列化支持。
- [isass-web-springmvc](isass-web-springmvc/README.md)：Spring MVC 服务端适配。
- [isass-adapter-springboot](isass-adapter-springboot/README.md)：Spring Boot 装配桥。

## 开发与验证

- 从根目录执行 `mvn install`，供下游使用最新本地构建。
- 服务使用 api/service/boot 三模块；跨上下文使用公开契约或事件，不直接访问对方 Repository。
- 测试优先覆盖权限、租户隔离、事务回滚、并发、协议与历史缺陷，不维护全量类名/入口快照。
- 提交格式：`type(scope): subject`。新机制简要更新所属模块 README，不另建长篇设计与使用文档。

## 记录

- [更新日志](docs/60.changelog/ChangeLog4.x.md)
- [路线图](docs/70.roadmap/roadmap.md)

框架稳定前，docs 仅保留上述两份文件，各不超过 2000 字符。具体行为以实现、测试和模块 README 为准。
