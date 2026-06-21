# Roadmap 未实现项分析

> 来源：docs/70.roadmap/2024.md（排除 L33-34）、2025.md、2026.md
> 说明：2025 L9 repository CTE 递归查询已于 2026-06-20 标记为暂不实施，不进入当前优先级队列。
> 说明：2026 L7 新增 isass-adapter-springboot 已于 2026-06-20 完成第一阶段，不再作为独立待办；2024 L16 核心模块与 Spring 完全解耦仍保留。`isass-core-*` 必须解耦 Spring，其他 `isass-*` 尽全力解耦 Spring。
> 分析维度：重要程度（高/中/低）× 实现难度（高/中/低）

---

## 统计

| 文件   | 未实现项数 |
| ------ |-------|
| 2024   | 24    |
| 2025   | 4     |
| 2026   | 21    |
| 合计   | 29    |

---

## 排序（按优先级从高到低）

### P0 — 高重要 + 高难度（核心架构）

1. **核心模块与 Spring 解耦（java 模块化）** [2024 L16]
   - 重要：高 — 决定框架能否脱离 Spring 体系使用，扩展桌面应用、IoT 等场景
   - 难度：高 — 需重构模块划分、新增 adapter 层、适配其他 IoC 框架
   - 进展：已新增 `docs/design/core-spring-decoupling-analysis.md`，逐项记录 `isass-core-*` Spring 使用点和迁移方案；`isass-core-common` main 源码已移除 Spring 编译依赖，排序、converter、异常映射、运行时 BeanProvider、LogUtil、ReflectUtils 已迁到 core 抽象 + Spring Boot adapter 桥接

2. **低代码子模块 DDD 重设计** [2024 L46]
   - 重要：高 — 低代码模块是框架核心竞争力
   - 难度：高 — 需迁移 v1/v2、设计 v3 接口、结合 DDD 重新实现
   - 设计：v3 不继承 v1/v2 的 service 排序链；本地/远程实现选择归调用路由层，缓存、事件、审计等归 operation interceptor，详见 `docs/design/nocode-v3-service-routing-cache.md`
   - 进展：`isass-nocode-core` 已新增 v3 operation pipeline、provider router、access handler、标准 CRUD 操作名和 access request 工厂、cache facade/cache operation、自定义实体标记接口、实体/字段元数据、实体元数据 SPI provider、实体注册表、Map/List 化查询条件、查询元数据校验器和空字符串查询策略；v2 自有包已补齐 `BatchSave`、`UnimplementedMethodException`、`IV2DbEntity`、`V2DbEntityConvert`，main 源码不再反向引用 `common.structure`；`isass-core-dependencies` 已管理 `isass-nocode-core` 版本；`isass-web-springmvc`、`isass-database-core`、`isass-database-mybatisplus`、`isass-adapter-springboot` 和首个适配项目 `isass-service-attachment` 已迁到 `vip.isass.framework.nocode.v2`

3. **新增 access 接入层（controller/socketio/kafka动态生成）** [2024 L53]
   - 重要：高 — 低代码统一接入层设计
   - 难度：高 — 需抽象多种接入方式、动态生成端点
   - 进展：纯 Java `NocodeAccessRequest` / `NocodeAccessHandler` / `NocodeCrudAccessRequests` 已落地；Spring MVC、socketio、kafka、定时任务等具体接入 adapter 尚未落地

4. **v3 代码生成器** [2024 L57]
   - 重要：高 — 低代码模块必备工具
   - 难度：中-高 — 根据 v3 接口设计生成逻辑
   - 进展：v3 实体/字段元数据和查询模型已落地，可作为后续生成器输入/输出契约；生成器尚未实现

5. **取消 db 实体，探索 ORM 无关实体** [2024 L50]
   - 重要：高 — 架构级提升，解耦 ORM
   - 难度：高 — 需 javassist/lombok 深度定制
   - 进展：已新增 `NocodeEntityDefinition` / `NocodeFieldDefinition` 作为 ORM 无关实体描述；ORM adapter 如何绑定 MyBatis Plus `TableInfo`、sqltoy 等仍待实现

6. **GraalVM 原生编译支持** [2025 L39]
   - 重要：高 — 云原生部署关键路线
   - 难度：高 — 需适配反射、资源、代理等限制

### P1 — 高重要 + 中难度 / 中重要 + 高难度

7. **重构异常模块** [2024 L38]
   - 重要：高 — 影响全框架异常体系
   - 难度：中 — 需设计新接口、兼容历史
   - 进展：`Resp.detailMessage` 与 `ExceptionAdvice` 双字段错误信息已落地；`IsassErrorController` 已完成 HTML/JSON 错误响应策略设计并修复无 `IStatusMapping` 时的 NPE 风险

8. **v3 通用 controller 动态生成** [2024 L54] (这个和P0 3 重复了)
   - 重要：高 — 低代码接入层关键子项
   - 难度：中 — Spring 动态注册端点已有成熟方案

9. **多个 ORM 框架同时支持** [2024 L51]
    - 重要：中 — 扩展性，方便切换 sqltoy
    - 难度：高 — ORM 抽象层设计复杂

10. **criteria 简化（删除 or/NotEqual 等字段）** [2024 L52]
    - 重要：中 — 提升编译速度、IDE 体验
    - 难度：中 — 需用 Map 替代并保持兼容
    - 进展：已新增 `NocodeQueryCriteria`、`NocodeQueryCondition`、`NocodeQueryGroup`，支持用条件列表和分组表达 equals、in、contains、or 等查询；已新增 `NocodeQueryValidator` 基于字段元数据校验未知字段、不可查询字段和不可排序字段；v2 生成模板尚未迁移到 v3 模型

11. **自定义实体继承 v3 接口** [2024 L48]
    - 重要：中 — 非标实体集成规范
    - 难度：中 — 接口设计 + 兼容已有实体
    - 进展：已新增 `NocodeEntity` 标记接口，业务实体可自行暴露 v3 元数据并生成 `NocodeEntityDefinition`；已新增 `NocodeEntityDefinitionProvider` + `ServiceLoader` 自动发现；ORM adapter 和业务项目实体验证尚未落地

### 暂不排期

1. **repository CTE 递归查询** [2025 L9]
   - 2026-06-20：暂时取消，不进入当前 roadmap 实现队列
   - 如后续重新启用，需要重新评估跨数据库 CTE 兼容策略、降级实现和测试矩阵

### P2 — 中重要 + 中难度

12. **JSR303 实体校验 + 分组校验** [2024 L61]
    - 重要：中 — 数据校验标准化
    - 难度：中 — 需集成 JSR303 + 优化响应消息

13. **service 事件监听（前置/后置）** [2024 L62]
    - 重要：中 — 业务扩展点
    - 难度：中 — 需设计事件模型与触发机制

14. **级联删除 / 关联表删除** [2024 L63]
    - 重要：中 — 低频但必要功能
    - 难度：中 — SQL 级联逻辑设计

15. **主从表关联查询** [2024 L70]
    - 重要：中 — 常用查询场景
    - 难度：中 — 需自动关联解析

16. **criteria 条件分组支持** [2025 L36]
    - 重要：中 — 复杂查询场景
    - 难度：中 — 条件树结构设计

17. **配置文件统一为 TOML 格式** [2024 L75]
    - 重要：中 — 配置统一管理
    - 难度：中 — 全量迁移，需兼容旧格式

18. **Docker 分层优化** [2024 L78]
    - 重要：中 — 镜像体积、部署效率
    - 难度：中 — 需研究 lib 外置、layer 复用

19. **文档项目 vuepress 重写** [2025 L5]
    - 重要：中 — 文档体验
    - 难度：中 — 搭建框架 + 主题配置

20. **文档自动同步** [2025 L6]
    - 重要：中 — 文档与代码同步
    - 难度：中 — 同步机制设计

### P3 — 低-中重要 + 低难度

21. **异常码按模块分类** [2024 L39]（这个和P1 7 重复）
    - 重要：中 — 异常规范化
    - 难度：低 — 配套异常重构统一规划

22. **新增级联controller分组方式** [2024 L55]
    - 重要：中 — 影响 API 文档结构
    - 难度：低 — 设计方案决策

23. **通用新增接口自动赋值** [2024 L64]
    - 重要：中 — 减少重复代码
    - 难度：低-中 — 注解/配置驱动

24. **空字符串查询条件优化** [2024 L68]
    - 重要：低 — 边界情况
    - 难度：低 — 条件判断逻辑
    - 进展：已新增 `NocodeBlankStringPolicy`，支持忽略空字符串或按空字符串查询；具体 access/ORM adapter 尚未接入

25. **数据库字段注释描述关系** [2024 L47]
    - 重要：低-中 — 辅助分析
    - 难度：低 — 注释规范补充

26. **分页对象优化** [2024 L49]
    - 重要：低 — 选型替换
    - 难度：低 — 统一分页对象
    - 进展：已新增纯 Java `NocodePageRequest` / `NocodePageResult`，后续 ORM adapter 可将 MyBatis Plus、sqltoy 等分页对象转换为统一 v3 模型

27. **时间字段改为 bigint(Long)** [2024 L65]
    - 重要：低 — 性能微优化
    - 难度：低 — 批量字段类型迁移

28. **formatTimestamp/setupTimestamp** [2024 L66]
    - 重要：低 — 调试辅助方法
    - 难度：低 — 纯新增接口方法

29. **开源许可证协议回顾** [2024 L74]
    - 重要：中 — 法律合规
    - 难度：低 — 调研 + 修改

### 已完成

1. **Resp 新增 detailMessage 字段** [2024 L40]
   - 2026-06-21：`Resp` 已新增 `detailMessage` 字段；`ExceptionAdvice` 在生产统一提示时将用户可见消息放入 `message`，将 traceId + 原始异常详情放入 `detailMessage`，便于开发排查且避免把内部细节混入用户提示。

2. **IsassErrorController 错误响应策略** [2024 L41]
   - 2026-06-21：新增 `docs/design/error-response-strategy.md`；`IsassErrorController` 对 `Accept: text/html` 的页面/静态资源错误只保留 HTTP 状态，对 JSON/API 请求返回 `Resp`，并修复状态映射列表为空时的 NPE 风险。

---

## 建议执行顺序

1. **P0（1-6）**：核心架构改造，建议分阶段实施
   - 先做 L16（Spring 解耦）和 L46（低代码模块），两者相互关联
   - 再做 L53、L54、L57（access 层 + controller + 代码生成）
   - L39（GraalVM）可作为独立专项
2. **P1（7-12）**：配套异常体系 + 低代码细节
3. **P2（13-22）**：功能增强、文档、适配层
4. **P3（23-32）**：低优先级，可穿插在空闲时间完成
