# V4 更新日志

## 4.0.0-SNAPSHOT

按能力合并历史记录；详细改动追溯 Git 历史。

- 文档收敛：docs 仅保留本日志和路线图；33 个模块以 README 简述职责，修复工作区、BSP 与项目模板的规则入口。
- 基础构建：升级 Java 25、Spring Boot 4 及配套依赖，统一模块命名与显式自动配置；core-common 保持纯 Java。根 POM 锁定 clean 插件 3.5.0。
- 服务入口：IEntrypoint 统一元数据、参数来源、访问策略、本地与 HTTP 客户端；NoCode 和自定义用例分别路由。非幂等请求不跨协议重试；动态 gRPC 保留预研，尚未部署 Server。
- NoCode：统一八个 CRUD 入口与事务执行器，支持 SuperCudReq、查询/写入生命周期、游标与树查询。普通分页统一返回 Page，数据库副作用在事务内、缓存刷新在提交后。
- 查询与关联：Criteria 使用 Java 属性、returnFields 和 loadRelated；JOIN/FROM/EXISTS/IN 递归构建，ORM 解析列名并绑定参数。MPJ 负责结果装配，关联分页按根实体统计并保留集合；不支持的写入形态明确拒绝。
- 关联写入：按请求出现性处理直接关系，MERGE 保留旧项、REPLACE 移除未提交项；关联更新要求唯一根 ID，删除按声明方向固定 ID 分批级联。
- 模型生成：EntityModelDefinition/EntityFieldDefinition 声明领域、枚举、类型与租户开关；EntityRelationDefinition 唯一定义关系。DDL 仅保留物理结构与说明；实体、Criteria 和关系元数据由生成器输出。
- 数据授权底座：DataReadPolicy/DataReadContext 为根、关联与子查询叠加范围，校验查询输入字段；DataReadProjection 防止补查策略键泄漏。隐藏字段由共享 Jackson 2/3 真正省略。Repository 增加事务记录锁与数据库条件判定，未适配时拒绝授权写入。
- JSON 与响应：JsonUtil 为统一入口，兼容 Jackson 3 与第三方 Jackson 2；高级响应投影不污染领域实体。统一异常映射、Resp.detailMessage、文件流响应头与错误状态。
- 数据库与迁移：Repository/表注册下沉数据库层；Liquibase 使用服务唯一 master/history，支持开发与发布迁移边界。租户与逻辑删除保护独立保留。
- 消息与网络：MQ 统一多源契约，适配 Kafka、Spring Event、Redis Stream/PubSub；网络模块显式装配并收敛构造器注入。
- 安全与部署：JWT/API Key 业务主体和内部 HMAC 主体分离；标准 assembly 统一部署结构，boot 不作为 Maven 依赖发布。运行时 OpenAPI 读取真实入口元数据。
