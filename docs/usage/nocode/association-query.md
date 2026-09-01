# NoCode 关联查询

关联由 DDL 表级注释声明，例如：

```text
[关联表-列表-SampleImage; cascadeDelete=true]
```

未填写的 `property/localKey/targetKey` 由生成器按实体和字段命名推断；推断有歧义时生成失败。关系是单向的，
目标实体只有在自己的 DDL 中声明对应关系后才生成反向属性。

关联标记的完整语法、默认推断、方向性级联和树形规则见
[数据库表设计规范](../database/table-design.md#liquibase-注释-dsl)。数据库不创建外键或数据库级联约束；
关系完整性和级联写入由应用事务、NoCode 协调器、唯一索引和业务校验保证。

前端通过 Criteria 的关联展开参数选择本次查询需要的关系。`page`、`cursorPage` 以及 Java 默认方法
`getOne/list/requireOne` 都经过同一个查询协调器；默认不展开。协调器按“当前一批实体 + 一个关系”批量查询
目标记录，不逐行查询。

直接关系使用属性名，多层关系使用点分路径：

```text
association.query=rolePermissions.permission
```

只提交上述路径时，框架自动先加载 `rolePermissions`，再对这一批 `RolePermission` 一次性加载
`permission`。每一层只执行一次批量查询，不产生逐行查询；父路径无需重复提交。嵌套关系自己的过滤条件
使用完整路径，例如：

```text
association.rolePermissions.permission.criteria.enabledFlag=true
```

一次展开多条路径时遵循统一集合 Query 格式，例如
`association.query=rolePermissions.permission,rolePrerequisites.prerequisiteRole`，不能重复提交多个
`association.query` 参数。

路径最大 16 层，且每一段都必须是对应实体通过 DDL 声明的关系。框架只展开显式路径及其父路径，不会因为
目标实体存在反向关系而继续递归。

写入时，实体关系属性的请求出现性决定是否处理：

- 未提交关系属性：不处理；
- `MERGE`：新增无 ID 对象、更新有 ID 对象、保留未提交的已有关系；
- `REPLACE`：请求值代表该方向最终结果，移除未提交的旧目标；
- 显式空集合：`MERGE` 不处理，`REPLACE` 清空；
- 单体关系显式 `null`：`REPLACE` 清空，`MERGE` 不处理。

直接关联写入只处理当前实体声明的一层关系，不递归保存任意深度对象图。跨聚合复杂流程由应用服务编排，
跨微服务或跨数据源关系不能使用通用关联写入。
