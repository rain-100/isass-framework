# V4 更新日志

## 4.0.0-SNAPSHOT

- 文档：仅保留日志、路线图，规则见模块 README。
- 基础构建：Java 25、Spring Boot 4，统一模块命名与显式自动配置；core-common 保持纯 Java。根 POM 锁定 clean 3.5.0；禁用 H2 测试与依赖。
- 服务入口：IEntrypoint 统一元数据、参数来源、访问策略、本地与 HTTP 客户端；NoCode 和自定义用例分别路由，非幂等请求不跨协议重试。HTTP FileStream 支持二进制及源连接释放。
- NoCode：统一八个 CRUD 入口与事务执行器，服务泛型仅要求 IEntity，无主键实体支持新增、普通查询及条件修改/删除；ID 方法、游标及 ID 关联能力运行时校验，生成器同步支持无主键表。SuperCudReq<E,C,PK> 的 deleteIds 按 PK 在入口反序列化。统一查询/写生命周期，数据库副作用在事务内、缓存刷新在提交后。
- 查询与关联：Criteria 使用 Java 属性、returnFields、loadRelated；Query 按属性绑定集合，修复重载将 returnFields 误作 JSON。JOIN/FROM/EXISTS/IN 递归构建，ORM 解析列名、绑定参数。MPJ 装配结果，关联分页按根实体统计并保留集合；
- 关联写入：递归保存、回填外键，固定条件统一读写。移除 writable/readOnly，仅在声明实体生成关联属性。嵌套增改删独立授权并经生命周期，ID-only 不重复修改；REPLACE/级联按实际 ID 删除，根关联更新加锁，失败回滚。权限执行器调用时解析，避免启动循环，授权失败即终止。
- 模型生成：EntityModelDefinition/EntityFieldDefinition 声明领域、枚举、类型、密钥保护与租户开关，EntityRelationDefinition 唯一定义关系及 Collection/List 类型。树根 NULL；审计用户 ID 独立推断校验。密钥生成 JSON 只写及 toString 排除；DDL 仅保留物理结构与说明，生成实体、Criteria 和关系元数据。
- 数据授权底座：角色数据过滤、固定租户边界与服务端生命周期约束独立叠加，覆盖根表、关联、子查询及统计。NoCode 缺少租户时拒绝租户数据，关联边界及平台豁免由开发者 SPI 声明。addReadRestriction 不修改客户端 Criteria；输入校验与内部补键隐藏防泄漏。Repository 支持事务记录锁及授权条件判定，未适配写入时拒绝。
- JSON 与响应：统一 JsonUtil，兼容 Jackson 3/第三方 Jackson 2，禁止浮点截断；JSON 集合和 Map 按字段泛型还原。枚举以 DELEGATING 读取编码，CSV 导出屏蔽 WRITE_ONLY 字段。高级响应不污染实体；统一异常、Resp.detailMessage、文件响应头及错误状态。
- 数据库与迁移：Liquibase 按服务隔离；启动自动建库，保留租户/逻辑删除保护，删库单独授权。初始化 JSON/XLSX/CSV 统一路由、仅新增、失败汇总；表格按实体解析 JSON，CSV 上传/模版/跨服务导出保留长 ID；实体目录含服务和上下文，本地及远程统一列出。
- 消息与网络：MQ 适配 Kafka、Spring Event、Redis Stream/PubSub；网络模块显式装配、构造注入。TCP/WebSocket 迁移 Netty 4.2 MultiThreadIoEventLoopGroup/NioIoHandler，保留 NIO 及默认线程策略，覆盖真实连接回归。
- 安全与部署：security-core 集中主体、JWT、授权及数据/租户 SPI，单向依赖 core-common；Spring Security 通过 SPI 对接 BSP，服务契约归 bsp-api。JWT 默认仅认证；authentication.load-roles-for-logging 可提前加载并打印角色，默认关闭，不主动加载 Permission。角色/功能权限按需分级加载并请求内复用，超级管理员跳过 Permission；null 区分未加载与仅角色，不新增状态字段，getter 无 I/O，加载失败终止。角色从上下文派生，删除 IsassGrantedAuthority 和显示名称；身份与内部 HMAC 分离。标准 assembly 统一部署结构，boot 不发布 Maven，OpenAPI 读取真实入口。
