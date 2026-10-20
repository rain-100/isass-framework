# isass-core-common

公共契约与工具。提供实体特征、Criteria、分页、异常及 JSON 支持；主体、JWT 和授权 SPI 位于 isass-security-core，本模块不反向依赖安全模块。

- JSON 统一使用 JsonUtil；serialization 包的 HiddenDataFields 和 Jackson 2/3 适配器负责省略隐藏字段，查询内部补键不得暴露到响应。
- Jackson 2/3 统一禁止浮点数截断为整数；整数类型仍支持整数字符串，避免主键等值发生有损转换。
- EntityAssociation 描述类型化关系及固定属性条件，不包含可写/只读开关；业务声明由生成器转换，查询与写入组件共用同一元数据，写入权限由授权组件校验。

[返回模块目录](../README.md)

- 树实体根节点的 parentId 统一为 NULL；markAsTopEntity() 清空 parentId，0 和字符串 "0" 均可作为普通节点 ID。
