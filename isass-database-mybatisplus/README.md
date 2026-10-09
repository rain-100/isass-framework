# isass-database-mybatisplus

MyBatis-Plus / MPJ 适配。注册实体表元数据，将 Criteria 转为 Wrapper，并执行 CRUD、JOIN 和分页。

- 列名由 ORM 从 Java 属性解析；条件值参数绑定。租户、逻辑删除和数据范围以 AND 叠加，不允许业务策略越级。
- 关联分页按根实体计数并保留集合；需要窗口函数。MySQL 不支持 FULL JOIN，SQL Server 授权写锁未适配；不安全或不支持的形态明确拒绝。

[返回模块目录](../README.md)
