# Nocode 通用关联查询设计

## 目标

标准 nocode 查询支持一对一和一对多关联数据回填，不依赖 ORM 注解，不引入 N+1 查询。关联
关系由 Liquibase 建表注释驱动代码生成，关联数据仅在显式查询时返回，不参与新增、修改或删除。

## 生成元数据

表注释支持以下标记：

```text
[关联表-列表-DictionaryItem]
[关联表-单体-DictionaryType]
```

生成器分别在当前实体生成：

```java
private Collection<DictionaryItem> dictionaryItems;
private DictionaryType dictionaryType;
```

不生成 `@NocodeAssociation` 等注解。生成器把关联描述写入现有动态实体元数据注册机制；实体
扫描据此排除关联字段的持久化列映射，同时保留 JSON 序列化。

第一版按命名约定推导关联键：列表关联使用 `当前实体.id -> 目标实体.当前实体名Id`；单体关联
使用 `当前实体.目标实体名Id -> 目标实体.id`。推导失败时，生成器必须失败并报告缺失字段。

## HTTP 查询协议

只查询关联成员但不加关联条件：

```text
association.query=dictionaryItems
```

关联 Criteria 使用前缀：

```text
association.<成员变量名>.criteria.<目标Criteria参数>=<值>
```

示例：

```text
association.dictionaryItems.criteria.orderBy=orderNum asc
association.dictionaryItems.criteria.selectColumns=id,itemCode,itemName,orderNum
association.dictionaryItems.criteria.pageNum=1
association.dictionaryItems.criteria.pageSize=100
association.dictionaryItems.criteria.enableFlag=1
association.dictionaryItems.criteria.itemCodeNotEqual=UNKNOWN
```

只要出现 `association.<成员>.criteria.*`，该成员自动视为已请求，无需同时传
`association.query`。参数后缀直接复用目标实体 Criteria 的既有绑定、类型转换、比较、排序和
投影能力，不创建另一套 DSL。

## 执行语义

1. 先执行主实体标准查询。
2. 收集当前结果的关联键。
3. 为关联实体构造目标 Criteria，并把 HTTP 参数前缀去除后绑定。
4. 强制附加关联键约束、当前租户约束和目标实体数据权限约束。
5. 单次批量查询关联实体；列表关联按外键分组回填，单体关联按键回填。

列表关联未传 `pageNum/pageSize` 时使用普通列表查询；传入分页参数时使用关联实体的分页查询，
只提取 `records` 并回填 `Collection`。当主查询有多条记录时，关联分页在所有主记录关联键组成的
全局结果集上执行一次，再按外键分组；不返回关联分页的 total、pageNum 或 pageSize。

`selectColumns` 必须自动补齐回填所需的关联外键；单体关联还必须补齐目标主键。客户端提供的
关联外键过滤值不能扩大范围，框架使用主查询键集合强制覆盖。

第一版只支持直接成员关联，不支持 `a.b.c` 嵌套路径；单体关联不接受分页参数。

## 字典拆表应用

`DictionaryType` 的表注释包含 `[关联表-列表-DictionaryItem]`。前端通过标准 nocode 查询：

```text
GET /bsp-service/dictionaryType/page
  ?bizType=iimage_asset
  &typeCode=AGE_STAGE
  &association.dictionaryItems.criteria.orderBy=orderNum asc
```

返回字典类型及其 `dictionaryItems`，取代原有
`findOptionsByTypeCode` 和 `getNameByTypeCodeAndOptionCode` 自定义方法。

