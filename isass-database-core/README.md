# isass-database-core

数据库与 Repository 契约。提供 ORM 无关 Repository、表命名及 Liquibase 配置支持。

- 服务仅有一个 Liquibase master 和 history；开发期结构调整与发布期增量迁移须明确区分。
- 受保护写入先在事务内锁定当前记录，再判范围和字段权限；未实现锁/条件授权的适配器必须明确拒绝。

[返回模块目录](../README.md)
