# isass-nocode-generator

模型与持久化代码生成。生成 Entity、Criteria、Repository、Mapper 和 CRUD Service 骨架。

- EntityModelDefinition/EntityFieldDefinition 定义领域、枚举、类型与租户开关；EntityRelationDefinition 是关系唯一手写来源。限界上下文取自表名，领域和子域只读取 Java 模型声明，不再解析表备注中的领域标记。
- 关系声明在哪个实体即仅生成该实体的关联属性，不隐式生成另一端，不提供 writable/readOnly 开关；嵌套增改删由关联对象对应的权限及业务校验控制。用 where(field,value) 固定多态关联类型；生成器验证目标字段并输出 EntityAssociation 元数据，运行时统一用于读取、嵌套写入及级联。
- 关联成员类型由 Java 关系声明保留：`one(...)` 生成单个实体，`many(...)` 生成 `Collection<T>`，`list(...)` 生成 `List<T>`，字段、getter、setter 和 builder 类型保持一致。已有 `many(...)` 声明不改变默认类型；`where(...)` 保留集合类型。`List` 不隐式添加 SQL 排序，业务顺序仍通过查询条件明确指定。
- DDL 只保留物理结构和说明；修改生成器/模板后重新生成 Entity 与 Criteria，不手改生成产物。已有 Service 的手写扩展不覆盖。
- Java 类型覆盖中的外部类型支持完整包名（如 `List<com.example.SnapshotNode>`）；生成时无同名冲突则导入并使用简单类名。
- `EntityFieldDefinition.secret("apiKey")` 声明密钥字段，生成 JSON 只写及 Lombok toString 排除注解；不改变数据库存储，不代替业务查询限制。内部复制不可通过 JSON 往返保留此类字段。
- 无主键表生成 IEntity 和普通 Criteria，不虚构 id 或 IIdCriteria；CRUD Service 的 PK 使用 Serializable 占位，仅执行不依赖 ID 的操作。
- 生成和测试均不使用 H2；已移除 H2 模型生成测试及依赖。
- Jackson 3 沿用 `com.fasterxml.jackson.annotation` 基础注解（jackson-annotations 2.x），不改为 tools 包；databind 的 JsonSerialize 等使用 `tools.jackson.databind.annotation`。内置枚举通过 JsonValue 输出整数编码，通过 JsonCreator 的 DELEGATING 模式按单个编码反序列化，不使用枚举序号。

例如，领域建模中的 `List<StoryboardShot> storyboardShots` 对应：

```java
EntityRelationDefinition.list("Work", "storyboardShots", "StoryboardShot", "id", "workId");
```

[返回模块目录](../README.md)

- 审计用户 ID 类型独立从 create_user_id/modify_user_id 推断，不跟随实体主键；两列类型不一致时生成失败。
