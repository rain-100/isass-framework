# isass-nocode-generator

模型与持久化代码生成。生成 Entity、Criteria、Repository、Mapper 和 CRUD Service 骨架。

- EntityModelDefinition/EntityFieldDefinition 定义领域、枚举、类型与租户开关；EntityRelationDefinition 是关系唯一手写来源。限界上下文取自表名，领域和子域只读取 Java 模型声明，不再解析表备注中的领域标记。
- 关系可用 readOnly() 声明仅供查询的引用，用 where(field,value) 固定多态关联类型；生成器验证目标字段并输出 EntityAssociation 元数据，运行时统一用于读取、嵌套写入及级联。
- DDL 只保留物理结构和说明；修改生成器/模板后重新生成 Entity 与 Criteria，不手改生成产物。已有 Service 的手写扩展不覆盖。
- 无主键表生成 IEntity 和普通 Criteria，不虚构 id 或 IIdCriteria；CRUD Service 的 PK 使用 Serializable 占位，仅执行不依赖 ID 的操作。
- 生成和测试均不使用 H2；已移除 H2 模型生成测试及依赖。
- Jackson 3 沿用 `com.fasterxml.jackson.annotation` 基础注解（jackson-annotations 2.x），不改为 tools 包；databind 的 JsonSerialize 等使用 `tools.jackson.databind.annotation`。内置枚举通过 JsonValue 输出整数编码，通过 JsonCreator 的 DELEGATING 模式按单个编码反序列化，不使用枚举序号。

[返回模块目录](../README.md)
