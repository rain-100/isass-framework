# isass-database-mybatisplus

数据读取与固定租户策略由 isass-security-core 提供，ORM 仅负责条件转换和 SQL 执行，不依赖 Spring Security。

MyBatis-Plus / MPJ 适配。注册实体表元数据，将 Criteria 转为 Wrapper，并执行 CRUD、JOIN 和分页。

- 列名由 ORM 从 Java 属性解析；条件值参数绑定。租户、逻辑删除和数据过滤以 AND 叠加，不允许业务策略越级。
- 无主键 IEntity 可新增、查询和按条件修改/删除；ID 方法要求 IIdEntity 与主键映射，不将无主键操作降级为无条件执行。
- 固定 TenantReadContext 覆盖根表、JOIN/FROM、EXISTS/IN、计数与存在性查询；JOIN 目标先过滤再关联。编译开发者固定条件时仅暂停角色授权校验，租户上下文继续生效；客户端条件与子查询不享有此豁免。
- TrustedReadContext 的生命周期约束按 Criteria 对象身份独立 AND 叠加，可使用不输出的字段；仅约束自身跳过字段输入校验，客户端 WHERE/ORDER/JOIN 仍校验，内部列不因此获得输出权限。exists 内部去除排序的 Criteria 副本显式继承原查询约束，其他查询不自动继承。
- 关联分页按根实体计数并保留集合；需要窗口函数。MySQL 不支持 FULL JOIN，SQL Server 授权写锁未适配；不安全或不支持的形态明确拒绝。
- 持久化 Collection/Map 字段使用完整 Java 泛型还原 JSON；无 ORM 注解的实体自动补建字段级 resultMap，Repository 直读与服务查询均保留嵌套元素类型。关联集合不作为 JSON 列，显式 resultMap 不被覆盖。
- 测试不使用 H2；已移除对应内存库集成测试和依赖。

[返回模块目录](../README.md)
