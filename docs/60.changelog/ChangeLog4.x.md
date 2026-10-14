# V4 更新日志

## 4.0.0-SNAPSHOT

按能力合并历史记录；详细改动追溯 Git 历史。

- 文档收敛：docs 仅保留本日志和路线图；各模块以 README 简述职责，修复工作区、BSP 与项目模板的规则入口。
- 基础构建：升级 Java 25、Spring Boot 4 及配套依赖，统一模块命名与显式自动配置；core-common 保持纯 Java。根 POM 锁定 clean 插件 3.5.0。
- 服务入口：IEntrypoint 统一元数据、参数来源、访问策略、本地与 HTTP 客户端；NoCode 和自定义用例分别路由。非幂等请求不跨协议重试；动态 gRPC 保留预研，尚未部署 Server。
- NoCode：统一八个 CRUD 入口与事务执行器，服务泛型仅要求 IEntity，无主键实体支持新增、普通查询及条件修改/删除；ID 方法、游标及 ID 关联能力运行时校验，生成器同步支持无主键表。SuperCudReq<E,C,PK> 的 deleteIds 按 PK 在入口反序列化。统一查询/写生命周期，数据库副作用在事务内、缓存刷新在提交后。
- 查询与关联：Criteria 使用 Java 属性、returnFields 和 loadRelated；Query 按属性定义绑定集合，修复便捷重载导致 returnFields 被误作 JSON。JOIN/FROM/EXISTS/IN 递归构建，ORM 解析列名并绑定参数。MPJ 负责结果装配，关联分页按根实体统计并保留集合；不支持的写入形态明确拒绝。
- 关联写入：按出现性递归保存并回填外键；只读引用、固定条件统一读取与写入。嵌套增改删独立授权及生命周期，ID-only 不重复修改；REPLACE/级联按实际 ID 删除，根关联更新加锁，整批事务回滚。入口及嵌套权限执行器在调用时解析，避免依赖入口目录的授权组件形成启动循环，拒绝仍终止调用。
- 模型生成：EntityModelDefinition/EntityFieldDefinition 声明领域、枚举、类型与租户开关；EntityRelationDefinition 唯一定义关系。删除旧 DDL 领域解析及配置说明，统一项目模板；DDL 仅保留物理结构与说明，实体、Criteria 和关系元数据由生成器输出。
- 数据授权底座：角色数据过滤、固定租户边界与服务端生命周期约束独立叠加，覆盖根表、关联、子查询及统计。NoCode 缺少租户时拒绝租户数据，关联边界及平台豁免由开发者 SPI 声明。addReadRestriction 不修改客户端 Criteria；输入校验与内部补键隐藏防泄漏。Repository 支持事务记录锁及授权条件判定，未适配写入时拒绝。
- JSON 与响应：JsonUtil 为统一入口，兼容 Jackson 3 与第三方 Jackson 2，禁止浮点数有损转整数；高级响应投影不污染领域实体。统一异常映射、Resp.detailMessage、文件流响应头与错误状态。
- 数据库与迁移：Repository/表注册下沉数据库层；Liquibase 使用服务唯一 master/history，支持开发与发布迁移边界。租户与逻辑删除保护独立保留。
- 消息与网络：MQ 统一多源契约，适配 Kafka、Spring Event、Redis Stream/PubSub；网络模块显式装配并收敛构造器注入。TCP/WebSocket 事件循环迁移到 Netty 4.2 MultiThreadIoEventLoopGroup/NioIoHandler，保留 NIO 通道和默认线程策略，补充真实连接回归测试。
- 安全与部署：security-core 集中主体、JWT、授权及数据/租户 SPI，单向依赖 core-common；Spring Security 通过 SPI 对接 BSP，服务契约归 bsp-api。JWT 默认仅认证；authentication.load-roles-for-logging 可提前加载并打印角色，默认关闭，不主动加载 Permission。角色/功能权限按需分级加载并请求内复用，超级管理员跳过 Permission；null 区分未加载与仅角色，不新增状态字段，getter 无 I/O，加载失败终止。角色从上下文派生，删除 IsassGrantedAuthority 和显示名称；身份与内部 HMAC 分离。标准 assembly 统一部署结构，boot 不发布 Maven，OpenAPI 读取真实入口。
