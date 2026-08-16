# NoCode 关联查询

关联由 DDL 表级注释声明，例如：

```text
[关联表-列表-SampleImage; cascadeDelete=true]
```

未填写的 `property/localKey/targetKey` 由生成器按实体和字段命名推断；推断有歧义时生成失败。关系是单向的，
目标实体只有在自己的 DDL 中声明对应关系后才生成反向属性。

前端通过 Criteria 的关联展开参数选择本次查询需要的关系。`page`、`cursorPage` 以及 Java 默认方法
`getOne/list/requireOne` 都经过同一个查询协调器；默认不展开。协调器按“当前一批实体 + 一个关系”批量查询
目标记录，不逐行查询，并使用访问路径阻止循环展开。

写入时，实体关系属性的请求出现性决定是否处理：

- 未提交关系属性：不处理；
- `MERGE`：新增无 ID 对象、更新有 ID 对象、保留未提交的已有关系；
- `REPLACE`：请求值代表该方向最终结果，移除未提交的旧目标；
- 显式空集合：`MERGE` 不处理，`REPLACE` 清空；
- 单体关系显式 `null`：`REPLACE` 清空，`MERGE` 不处理。

完整的 DDL、级联、树形和事务规则见
[NoCode 级联、关联与树形 CRUD 设计](../../design/nocode-crud-scenario-implementation.md)。
