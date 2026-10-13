# isass-nocode-generator

模型与持久化代码生成。生成 Entity、Criteria、Repository、Mapper 和 CRUD Service 骨架。

- EntityModelDefinition/EntityFieldDefinition 定义领域、枚举、类型与租户开关；EntityRelationDefinition 是关系唯一手写来源。
- 关系可用 readOnly() 声明仅供查询的引用，用 where(field,value) 固定多态关联类型；生成器验证目标字段并输出 EntityAssociation 元数据，运行时统一用于读取、嵌套写入及级联。
- DDL 只保留物理结构和说明；修改生成器/模板后重新生成 Entity 与 Criteria，不手改生成产物。已有 Service 的手写扩展不覆盖。
- 无主键表生成 IEntity 和普通 Criteria，不虚构 id 或 IIdCriteria；CRUD Service 的 PK 使用 Serializable 占位，仅执行不依赖 ID 的操作。

[返回模块目录](../README.md)
