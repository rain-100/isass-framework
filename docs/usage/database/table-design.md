# 数据库表设计规范

## 表命名

所有业务表必须使用以下严格命名规则：

```text
{service}_{context}_{entity}
```

| 段 | 含义 | 示例 |
| --- | --- | --- |
| `service` | 服务标识 | `bsp`、`trade` |
| `context` | DDD 限界上下文标识 | `auth`、`attachment`、`order` |
| `entity` | 当前上下文内的实体标识 | `user`、`file`、`order_item` |

示例：

```text
bsp_auth_user
bsp_auth_role
bsp_attachment_file
bsp_attachment_icon
bsp_log_request_log
trade_order_order
trade_order_order_item
```

该规则没有省略例外：即使 `service` 与 `context` 名称相同，也必须完整保留三段，例如
`order_order_order`。这保证表名的归属和含义始终清晰、无歧义。
