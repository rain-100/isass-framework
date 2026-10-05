# NoCode 关联查询实现验证记录

日期：2026-09-23。验证对象是框架新增适配，不是未经扩展的 MPJ 默认行为。

## 环境与范围

- Spring Boot 4.0.8 / MyBatis-Plus 3.5.17 / MPJ 1.5.9 / Java 25。
- 框架根目录执行 `mvn -T 1 install`，使用现有 settings；不额外引入 Central 配置。
- BSP 经 IDEA 同步 Maven、编译并启动现有 BspApp 配置，实际 MySQL 版本为 8.0.45。
- 实库验证通过 IDEA 调试表达式调用现有 Repository Bean，先经过 Criteria JSON 反序列化，再执行真实 JDBC。
- 图标分组表无记录，改用现有角色、角色权限、权限表；验证表达式只有查询，没有创建/修改/删除业务记录。
- BspApp 启动仍会执行已有 AuthInit 和存储容量刷新。本次未删库、未执行 DDL、未修改数据库连接。

## 已验证结果

| 检查 | 结果 |
| --- | --- |
| 框架全模块 install | 通过，含新增核心、HTTP 和 ORM 回归 |
| BSP 全量 install | 通过，使用已安装的新框架依赖 |
| API 文档服务 / Asset 全量 test | 均通过；Asset 10 个实体 Mapper 按生成模板迁移 |
| Asset IDEA 编译 | Maven 同步后通过，无需业务重复声明 MPJ |
| 嵌套 Criteria 协议往返 | HTTP 实际客户端经 MockMVC 控制器及响应、gRPC 本机真实端口往返通过；中文及特殊字符、JOIN/WHERE/FROM/loadRelated 树保持一致 |
| BSP 定向测试 | IconGroupNocodeAssociationTest、RoleLifecycleListenerTest、UserTenantServiceTest，共 11 项通过 |
| IDEA 编译和 BspApp 启动 | 同步 Maven 后成功，未在 BSP 重复添加 MPJ 依赖 |
| LEFT JOIN 按实体分页 | 每页 1 个角色，三页各 1 条，total=3；第三页完整装配 5 个 RolePermission |
| LEFT count / exists | 分别为 3 / true，与分页一致 |
| INNER JOIN | total=1，关联筛选生效 |
| CROSS JOIN | 无 ON 的 SQL 在 MySQL 成功执行；每页 1 条，total=3 |
| 嵌套 JOIN | Role → RolePermission → Permission，最内层实体非 null |
| EXISTS / NOT EXISTS | 相关字段比较后 count=1 / 2 |
| IN / NOT IN | 单列 roleId 子查询后 count=1 / 2 |
| FROM + RIGHT JOIN | 根输入条件 id=-1，得到 5 个独立空根单位；每页 1 条、total=5，根 id=null，关联仍存在 |
| MySQL FULL JOIN | SQL 执行前明确拒绝：mysql 不支持 FULL JOIN |
| 带 OR 的相关 EXISTS | 最新框架重启 BSP 后，JSON Criteria 查询得到 count=1；实际 SQL 为 `(effect='ALLOW' OR effect='DENY') AND t.id=tst1.role_id` |

隔离 H2 JDBC 回归另覆盖：逻辑删除、CROSS 无 ON、ON 单字段常量、嵌套派生表、选列、单体关系多目标失败、非默认嵌套分页拒绝。未将 H2 MySQL 模式当作实库证据。

## 验证中修复的问题

- 2026-09-24 转换职责归并：删除独立 MpjCriteriaCompiler，将递归转换逻辑合并到 WrapperUtil，原编译测试改从 getQueryWrapper 入口验证。框架根目录 `mvn -T 1 clean install` 于 13:57 通过，数据库模块 21 项测试全部通过，产物不再包含旧编译器类。IDEA 构建仍被既有 Hutool/Jackson 等依赖缺失阻塞；未重启 BSP 或修改业务数据。

- 2026-09-24 统一查询 Wrapper：删除 getReadWrapper/getCountWrapper 和普通/关联构建分流，Criteria 查询统一使用 getQueryWrapper。单表筛选/投影/分页/count/exists/逻辑删除、关联查询及删除拒绝边界的 JDBC 回归通过；Wrapper 条件测试、MPJ 编译测试、存在性测试各 4 项通过。框架全量 `mvn -T 1 install` 于 13:41 通过，IDEA 构建仍被既有依赖缺失阻塞。本轮未重启 BSP，未将隔离 JDBC 回归视为新的 MySQL 实库验收。

- 2026-09-24 loadRelated 批次复用：取消逐批复制，复用原 Criteria 及一个框架 IN 节点，成功/异常清理临时 IN 并恢复分页。覆盖 1001 个主表 ID 分批、ORM 缩小页大小后的续页、业务同字段 IN 与 OR 分组保留、同一请求重复执行、EmptyCriteria 一次物化及失败重试。框架全量 `mvn -T 1 install` 通过，AssociationCoordinatorTest 6 项、CrudQueryExecutorTest 8 项及协议/JDBC 回归通过；IDEA 构建仍被既有依赖缺失阻塞，未重启 BSP。

- 查询执行器引用语义：按最新约定取消 CrudQueryExecutor 入口及游标深复制，原 Criteria 保留生命周期/补键/游标修改；此前“正式查询入口隔离”验证记录仅代表历史行为。本轮 `mvn -T 1 install` 于 23:56 全量通过，CrudQueryExecutorTest 8 项通过，包含嵌套对象引用、生命周期前后及 Repository 使用同一条件、游标实际参数可见和调用方显式 copy 隔离；关联、协议及 JDBC 回归通过。IDEA 构建仍被既有依赖缺失阻塞，未重启 BSP。

- 静态元数据缓存与条件工厂补充验证：框架根目录再次执行 `mvn -T 1 install` 通过；并发泛型字段/setter 绑定、显式子查询副本隔离、工厂 JSON 往返、HTTP/gRPC 往返、布尔分组消费时失败、JDBC 分组结果和双字段 ON 均通过。缓存扫描排除桥接 getter，避免实体 ID 从 Long 退化为 Integer；未进行性能基准，不宣称具体提速比例。本轮未重启 BSP 或改动业务数据。
- 构造引用语义补充验证：取消构造阶段隐式复制后，框架根目录 `mvn -T 1 install` 于 23:32 再次通过。覆盖原对象/集合修改可见、EXISTS/IN 便捷修改、EmptyCriteria 独立物化、显式 copy 和正式查询入口对监听器修改的隔离；协议与 JDBC 回归通过。IDEA 构建仍因 Hutool/Jackson/SLF4J 等依赖缺失失败，未通过增加重复依赖绕过。本轮未重启 BSP。
- EntrypointHttpServer 新增构造方法后，组件扫描无法选择构造器：为带 QueryParamConverter 的构造器声明注入，并增加 Spring 上下文回归。
- Criteria 便捷筛选 setter 的反射调用把 Jackson 泛型返回值推断为 Object[]：先接收为 Object，再作为单个参数 invoke；新增根/嵌套 id 筛选回归。
  最新框架安装并重启 BSP 后，直接使用 `fromCriteria: {entityType: "role", id: -1}` 的实库验证通过。
- JOIN 目标的 ON 标量条件使用目标实体元数据及 MPJ 别名，避免错误引用主表字段。
- loadRelated 的业务条件整体分组后再 AND 关联键，避免 OR 改变批次范围。
- EXISTS/NOT EXISTS 便捷方法同样先分组已有 WHERE 再追加关联键，避免 OR 扩大子查询范围；隔离 JDBC 回归覆盖便捷方法，BSP 实库验证对应 JSON 条件树。
- 外连接分页用目标主键打破空根行的排序并列，不仅按全部为 null 的根 ID 排序。

## 边界与未验收项

- 实库验证走 Repository；无登录主体的受保护 Service 正常拒绝调用，未绕过鉴权。本记录不宣称带登录凭证的 HTTP/gRPC 端到端验收。
- 数据权限与租户过滤按用户要求不在本次实现，仍由后续 Criteria/生命周期机制负责。
- FULL 在 MySQL 上只验证拒绝路径；其他数据库服务器未验证。
- 窗口函数分页满足根单位/完整关联的结果合同，但尚未做百万级性能测试或根范围提前裁剪优化。
- 通用 DTO/行投影、资源阈值仍按设计中的待确认项处理。
- BSP 51 个、Asset 10 个实体 Mapper 已迁移；范围外仍使用旧 BaseMapper 的业务项目需要按新模板迁移后再构建，本记录不代表它们已全部验收。
- 框架 IDEA 独立编译仍报告 Hutool、SLF4J 等既有传递依赖缺失，与根 Maven install 成功不一致；本次未改写 Maven 父依赖规则或为各模块重复添加依赖。BSP IDEA 启动与 Asset IDEA 编译通过，框架 IDE 模型问题单独保留。

最终框架根构建 `mvn -T 1 install` 于 20:12 通过；BSP 于 20:16 用最新依赖重新启动，20:18 完成补充查询并恢复运行，未遗留 agent 的暂停断点或终端启动进程。
