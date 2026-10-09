# isass-nocode-generator

模型与持久化代码生成。生成 Entity、Criteria、Repository、Mapper 和 CRUD Service 骨架。

- EntityModelDefinition/EntityFieldDefinition 定义领域、枚举、类型与租户开关；EntityRelationDefinition 是关系唯一手写来源。
- DDL 只保留物理结构和说明；修改生成器/模板后重新生成 Entity 与 Criteria，不手改生成产物。已有 Service 的手写扩展不覆盖。

[返回模块目录](../README.md)
