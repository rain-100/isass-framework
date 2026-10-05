# MPJ / Spring Boot 4 验证记录

日期：2026-09-23。结论：当前组合的基础自动配置和多项真实查询可以运行，但未满足关联查询重构的全部合同，暂不据此开展全面替换。

> 这是扩展实现前的原始探针记录，保留当时结果。用户随后批准继续适配，不以个别用例失败阻塞整体重构。
> 后续实现及真实 BSP/MySQL 验证见 [关联查询实现验证记录](nocode-related-query-verification-2026-09-23.md)。

## 1. 环境与验证边界

- Spring Boot 4.0.8、MyBatis-Plus 3.5.17、MPJ 1.5.9、JSQLParser 4.9、Java 25。
- 使用项目当前依赖和 `SqlSessionConfig`，通过 ApplicationContextRunner 加载 MP/MPJ 自动配置。
- 执行插件链含框架的 MybatisPlusInterceptor（乐观锁、分页）及 MPJInterceptor；额外的 SqlTrace 只记录实际提交 JDBC 的 SQL，不提前构造 BoundSql。
- 数据库：本地 H2 2.4.240 隔离内存库；另外启用 H2 MySQL 模式并指定 MP 的 MySQL 分页方言，完整复跑相同用例。
- **MySQL 模式不是 MySQL 服务端。未实测 MySQL、PostgreSQL、达梦服务器及生产启动配置。**
- 未修改或访问 BSP/Asset 数据；每次生成唯一内存库，保活连接关闭后自动销毁。测试 DDL/DML 仅用于创建夹具，所有被测查询由 ORM 生成。
- 未更换依赖、修补 MPJ 或实现新的 Criteria 协议。程序为诊断探针，不是关联查询功能的验收通过证明。

GitHub [Issue #352](https://github.com/yulichang/mybatis-plus-join/issues/352) 页面仍为 Open，未列具体版本和堆栈。
本报告只陈述当前环境实测结果，不宣称解决该 issue，也不将所有缺口归因于 Spring Boot 4。

## 2. 用例结果

两种方言运行结果一致：18 个检查，10 个正向检查通过，1 个负向对照符合预期，7 个检查未满足设计合同。
程序输出中的 `passed=11` 包含负向对照，不能解读为租户隔离已通过。

| 检查 | 结果 | 观察 |
| --- | --- | --- |
| BaseMapper 逻辑删除 | 通过 | 删除标记记录被排除 |
| LEFT JOIN 集合装配 | 通过 | 两个根对象；第一个关联 11、12，第二个为空列表；两侧显式租户条件有效 |
| INNER JOIN 集合装配 | 通过 | 一个根对象及两个目标对象 |
| 普通 JOIN 分页满足实体分页合同 | 不满足 | 每页一条时只装配一个子对象，原有两个被截断；total=3 是展开行数，不是两个根对象 |
| pageByMain 第一页 | 通过 | total=2，第一个根对象的两个子对象完整 |
| pageByMain 第二页，全新 Wrapper | 失败 | SQL 残留 `MPJ_Param_i_s2_MPJ_Param_i`，数据库报不存在的列 |
| FROM 派生表 + JOIN 目标派生表 | 通过 | 根过滤和目标过滤生效，关联选列正确装配 |
| 相关 EXISTS | 通过 | 正确引用外层主键，仅返回存在可见子项的根 |
| NOT EXISTS / IN / NOT IN 子查询 | 通过 | 按测试数据返回预期根对象；本用例未覆盖 NULL/空集合的所有边界 |
| 默认逻辑删除下 RIGHT 未匹配行 | 不满足 | 外层 `WHERE t.deleted=false` 丢弃两个空根行 |
| 关闭自动逻辑过滤后的 RIGHT 装配 | 通过 | 返回根 1 和两个独立 null-ID 对象，分别关联 91、92；仅验证装配，不能作为安全方案 |
| RIGHT pageByMain，外层带子表条件 | 失败 | COUNT 删除 JOIN 却保留 `t1.tenant_id`，产生不存在的别名引用 |
| RIGHT 两侧输入预过滤 | 通过 | 通过派生表保留逻辑删除/显式租户限制，并返回两个未匹配目标行 |
| RIGHT 输入预过滤 + pageByMain | 不满足 | 移除子表排序后可执行；返回三个结果对象，但 total=2，不符合“匹配根 + 每个空根行”的计数单位 |
| 只限制根租户的负向对照 | 符合预期 | 子表另一租户的 14 仍返回，证明根条件不会自动传播到目标实体 |
| 单体关联多个目标时拒绝 | 不满足 | 不抛基数异常，返回一个根对象，child 被赋值为 12；原有 11 被覆盖 |
| 结构化 CROSS 执行 | 失败 | 仍生成尾部空 ON，语法错误 |
| 原乐观锁插件 | 通过 | 首次更新成功，旧版本更新影响零行，没有覆盖新值 |

“不满足”不等于全部都是 MPJ 缺陷：普通分页是行分页语义，单体关联基数拒绝、空根行计数以及关联数据权限属于 ISASS 的更强合同，需要额外实现。

## 3. 已定位的分页问题

第二页即使新建 Wrapper 也复现，排除了跨页复用 Wrapper 的影响。

MySQL 方言实际输出片段：

```sql
FROM (
  SELECT * FROM probe_parent t
  WHERE t.deleted = false AND (t.tenant_id = ?)
  ORDER BY t.id ASC
  LIMIT ?, MPJ_Param_i_s2_MPJ_Param_i
) t
LEFT JOIN probe_child t1
  ON (t1.parent_id = t.id AND t1.tenant_id = ? AND t1.deleted = false)
```

MPJ 1.5.9 `DialectWrapper.buildPaginationSql` 的源码与此一致：

- `count` 来自原始 SQL 的问号数量，本例为 2（根 tenantId、JOIN tenantId）。
- 生成根分页子查询后，本例有 3 个参数（根 tenantId、offset、limit）。
- 还原分页占位符的循环仍采用 `i < count`，只还原两个，留下第三个内部标记。

该问题发生在 SQL 生成阶段，不能简单归为 H2 不支持 MySQL 语法，也不依赖 Spring Boot 的 Bean 注册失败。
本轮只定位，未修改该依赖或尝试用额外假条件规避。

RIGHT 分页还有不同的语义问题：`pageByMain` 以根表分页，不天然支持右侧保留的未匹配行。
子表条件/排序放进已经移除 JOIN 的根查询会产生别名错误；即使移除这些条件/排序，统计单位也不符合设计。

## 4. 租户与数据权限结论

当前 `SqlSessionConfig` 没有注册 TenantLineInnerInterceptor 或 DataPermissionInterceptor。
既有 BSP 业务权限与租户范围主要由 NoCode `CrudQueryLifecycleListener.beforeQuery` 追加 Criteria 条件；
例如 TenantAppLifecycleListener 会约束当前租户。这与“MPJ 自动为所有 JOIN 目标执行该实体权限规则”不是一回事。

探针中的租户条件由测试显式传给 Wrapper；负向对照已证实只约束根表不能自动约束目标表。
本轮没有调用 BSP 身份认证/授权入口，**未完成实际账号、目标实体生命周期、嵌套关联范围的端到端验证**。
后续必须按设计实现逐目标范围准备和正确的输入侧过滤后，再验收；不能因为直接 Mapper 查询成功就认定业务权限安全。

## 5. 尚未覆盖

- 实际 MySQL/PostgreSQL/达梦服务器、FULL JOIN 的数据库执行。
- 动态数据源、生产 Mapper 扫描和完整服务启动；本轮为隔离的自动配置上下文。
- 框架无 ORM 注解实体通过 TableMetaRegistrar 接入 MPJ 的完整链路（本探针实体使用 MP 注解）。
- 动态字段/重复自连接、全部嵌套组合、动态表名非默认策略、自定义 Injector 合并。
- 新 Criteria 的 JSON/Query/HTTP/gRPC 往返及自动 resultProperty 推断；这些重构尚未实施。
- 真实租户/数据权限监听器对所有关联实体的覆盖。

## 6. 复现

程序：[IsassMpjDatabaseProbe.java](probes/IsassMpjDatabaseProbe.java)。仅依赖现有项目类路径和独立 H2 驱动，没有把 H2 加入项目 POM。

在框架根目录执行（要求 JDK 25；本机 H2 2.4.240 已在 Maven 本地仓库）：

```bash
mvn -T 1 install
mvn -pl isass-database-mybatisplus dependency:build-classpath -Dmdep.outputFile=/tmp/isass-mpj-probe-classpath

mpj_probe_cp="$(< /tmp/isass-mpj-probe-classpath):$HOME/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar:$PWD/isass-database-mybatisplus/target/classes"
java --class-path "$mpj_probe_cp" docs/design/probes/IsassMpjDatabaseProbe.java
java --class-path "$mpj_probe_cp" docs/design/probes/IsassMpjDatabaseProbe.java mysql-dialect
```

程序捕获各用例异常并继续收集证据；退出码 0 只表示探针运行完毕，必须检查 `FINDING` 和 `SUMMARY`，不作为 CI 通过标志。
SQL 日志来自执行期 StatementHandler，不在查询前调用 getBoundSql，以免提前缓存缺少 MPJ 关联投影的 SQL。

## 7. 后续决策

基础启动验证不支持“当前 MPJ 完全无法在 Boot 4 下运行”的结论；但现有组合也不能直接满足完整重构合同。
在扩大实现前，需先确定分页参数修复、CROSS 扩展和外连接分页/装配的处理方案。
继续遵循两份设计中“扩展 ORM 或切换依赖前先确认”的边界，本轮没有执行上述修改。
