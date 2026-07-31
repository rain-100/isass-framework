# 零代码关联查询使用说明

零代码关联查询用于在查询主实体时，一并返回其关联的单体或列表数据。例如查询字典类型时，同时返回其字典项。

关联查询只影响查询响应，不改变主实体和关联实体的标准 CRUD 接口。关联关系由建表语句的**表注释**声明，代码生成后作为普通模型字段和框架元数据存在，不使用 ORM 注解。

## 1. 声明关联关系

在主表的表注释中增加以下标识：

| 标识 | 生成结果 | 适用场景 |
| --- | --- | --- |
| `[关联表-列表-目标实体]` | `private Collection<目标实体> 成员名;` | 一对多，例如字典类型包含多个字典项 |
| `[关联表-单体-目标实体]` | `private 目标实体 成员名;` | 多对一或一对一，例如字典项关联一个字典类型 |

示例：

```sql
-- 字典类型(按业务分类维护字典项) [关联表-列表-DictionaryItem]
CREATE TABLE bsp_config_dictionary_type (...);

-- 字典项(字典类型的可选值) [关联表-单体-DictionaryType]
CREATE TABLE bsp_config_dictionary_item (...);
```

生成后的模型示意：

```java
public class DictionaryType implements IEntity<DictionaryType> {
    private Long id;
    private String bizType;
    private String typeCode;
    private Collection<DictionaryItem> dictionaryItems;
}

public class DictionaryItem implements IEntity<DictionaryItem> {
    private Long id;
    private Long dictionaryTypeId;
    private DictionaryType dictionaryType;
}
```

框架当前按以下约定推导关联键：

- 列表关联：主实体的 `id` 对应目标实体的 `主实体名Id` 字段。
- 单体关联：主实体的 `目标实体名Id` 字段对应目标实体的 `id`。

例如 `DictionaryType.dictionaryItems` 使用 `DictionaryType.id -> DictionaryItem.dictionaryTypeId`；`DictionaryItem.dictionaryType` 使用 `DictionaryItem.dictionaryTypeId -> DictionaryType.id`。关联键不符合此约定时，应先调整实体命名或扩展关联元数据，不要在前端拼接条件模拟关联。

## 2. 请求关联数据

以下标准查询接口支持关联查询：`/{id}`、`/exception/{id}`、`/1/criteria`、`/warn/criteria`、`/exception/criteria`、`/criteria`、`/page`、`/all`。

通过 `association.query` 指定需要返回的关联成员。多个成员使用英文逗号分隔。

```text
GET /bsp-service/dictionaryType/page?bizType=iimage_asset&typeCode=AGE_STAGE&association.query=dictionaryItems
```

响应中的主记录会包含关联字段：

```json
{
  "success": true,
  "data": {
    "records": [
      {
        "id": "2080000000000000001",
        "bizType": "iimage_asset",
        "typeCode": "AGE_STAGE",
        "dictionaryItems": [
          {
            "id": "2080000000000000002",
            "itemCode": "INFANT",
            "itemName": "婴儿"
          }
        ]
      }
    ]
  }
}
```

未传 `association.query` 时，接口保持原有响应，不查询也不返回关联数据。

## 3. 关联实体查询条件

关联成员可使用其目标实体的 Criteria 参数。参数格式固定为：

```text
association.<关联成员名>.criteria.<目标实体 Criteria 参数>=值
```

例如只查询启用的字典项，并指定排序、返回字段：

```text
GET /bsp-service/dictionaryType/page?
  bizType=iimage_asset&
  typeCode=AGE_STAGE&
  association.dictionaryItems.criteria.enableFlag=1&
  association.dictionaryItems.criteria.orderBy=orderNum asc&
  association.dictionaryItems.criteria.selectColumns=id,dictionaryTypeId,itemCode,itemName,orderNum
```

常用参数示例：

```text
association.dictionaryItems.criteria.itemCode=INFANT
association.dictionaryItems.criteria.itemCodeNotEqual=UNKNOWN
association.dictionaryItems.criteria.orderBy=orderNum asc
association.dictionaryItems.criteria.selectColumns=id,dictionaryTypeId,itemCode,itemName,orderNum
association.dictionaryItems.criteria.pageNum=1
association.dictionaryItems.criteria.pageSize=100
```

- `criteria` 后面的字段和后缀与目标实体的标准 Criteria 保持一致，以 OpenAPI 文档为准。
- 传递任意 `association.<成员>.criteria.*` 参数时，框架会自动视为已选择该关联成员，无需重复传递 `association.query`。
- 使用 `selectColumns` 时，必须保留目标实体用于关联回填的字段。例如查询 `dictionaryItems` 时，必须包含 `dictionaryTypeId`；否则框架无法将字典项回填到对应的字典类型。
- 关联查询仍会执行租户隔离、数据权限和逻辑删除等现有查询规则。

## 4. 关联列表分页语义

可以为列表关联传递 `pageNum`、`pageSize`：

```text
association.dictionaryItems.criteria.pageNum=1
association.dictionaryItems.criteria.pageSize=100
```

此分页针对本次主查询涉及的全部关联记录执行。框架只取关联查询结果中的 `records`，再按关联键回填到每个主实体的集合字段：

- 响应中不返回每个关联集合的 `total`、`pageNum`、`pageSize` 等分页元数据。
- 不保证每个主实体都分别取得同样数量的关联记录。
- 需要单独展示关联列表、总数或独立分页时，应直接调用关联实体自身的标准分页接口。

单体关联不支持分页参数。

## 5. 使用边界

- 当前仅支持直接关联成员，例如 `dictionaryItems` 或 `dictionaryType`；不支持 `a.b.c` 形式的嵌套关联加载。
- 关联字段用于查询响应投影，不应作为新增、更新主实体时的级联保存数据。
- 关联表的新增、编辑、删除仍通过其自身标准 CRUD 接口或业务应用服务完成。
- `association.query` 中的成员名必须与生成实体的字段名一致。
- 当关联数据量较大时，优先通过 `criteria.selectColumns` 只返回页面实际需要的字段，并避免在列表页无条件加载大集合。

## 6. 前端调用建议

页面首次只需要主列表时，不传关联参数；仅在详情页、级联下拉框或确实需要关联内容的列表页添加关联参数。这样可以避免不必要的响应体和查询开销。

以资产标签下拉框为例，前端可先查询字典类型及字典项：

```text
GET /bsp-service/dictionaryType/page?
  bizType=iimage_asset&
  typeCode=AGE_STAGE&
  association.dictionaryItems.criteria.enableFlag=1&
  association.dictionaryItems.criteria.orderBy=orderNum asc
```

得到 `dictionaryItems` 后，前端直接使用 `itemCode` 作为业务值、`itemName` 作为展示文案；业务表持久化时保存字典项 `id`，避免冗余保存名称。
