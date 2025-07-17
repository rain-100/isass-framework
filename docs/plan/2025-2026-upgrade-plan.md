# ISASS框架2025-2026年升级重构计划

## 重构目标

基于v3.x版本（JDK8 + Spring Boot 2）进行全面升级重构，目标升级到JDK21 + Spring Boot 3，并解决现有架构问题。

## 1. 基础环境升级

### 1.1 JDK和Spring Boot版本升级
- [ ] JDK升级到21，Spring Boot升级到3.x 【工作量：3人日】
- [ ] 所有依赖升级到最新版本 【工作量：4人日】

### 1.2 模块重命名和结构优化
- [x] 模块重新命名，super改为isass，各模块的核心代码模块以core结尾
- [ ] 使用Java模块化技术重新划分模块，核心模块与Spring解耦 【工作量：5人日】
- [ ] 业务项目通过新建isass-framework-spring-starter模块与Spring集成 【工作量：15人日】

## 2. 框架内部依赖注入重构

### 2.1 现状与问题
- 框架内部各模块原本通过@Resource注解，由Spring进行依赖注入，导致对Spring强依赖
- 限制了框架的适用场景，无法用于非Spring环境（如命令行工具、桌面应用等）

### 2.2 重构方案
- **框架内部所有子模块全部移除@Resource等Spring注解，统一采用Java SPI机制进行依赖注入** 【工作量：20人日】
- 框架内部各模块通过接口+SPI发现机制进行解耦和自动装配，不依赖任何IOC框架
- 业务层如需IOC功能，可通过依赖ioc目录下的starter模块快速集成Spring等IOC框架

**优势：**
- 框架内部完全解耦，支持任何Java环境
- 业务应用可自由选择IOC框架（Spring、Micronaut、Quarkus等）
- 支持非Web服务开发（命令行工具、桌面应用）
- 框架更加轻量和灵活

**目录结构建议：**
```
📁 ioc/
├── 📦 isass-framework-spring-starter      # Spring Boot快速接入
├── 📦 isass-framework-micronaut-starter   # Micronaut快速接入（预留）
├── 📦 isass-framework-quarkus-starter     # Quarkus快速接入（预留）
└── 📦 isass-framework-solon-starter       # Solon快速接入（预留）
```

## 3. 数据库管理重构

### 3.1 当前问题
- Flyway需要为不同数据库编写不同的SQL脚本
- 多数据库支持工作量大，维护成本高

### 3.2 重构方案
**替换为Liquibase：**
- 一份XML配置支持多种数据库
- 运行时自动生成对应数据库的SQL
- 支持数据库版本管理和回滚
- 大幅降低多数据库支持成本
- [Liquibase 还是 Flyway](https://developer.aliyun.com/article/1130804)

**模块结构调整：**
```
📁 database/
├── 📦 isass-framework-database-core
├── 📦 isass-framework-database-liquibase
├── 📦 isass-framework-database-mysql
├── 📦 isass-framework-database-postgresql
├── 📦 isass-framework-database-dameng
├── 📦 isass-framework-database-elasticsearch
├── 📦 isass-framework-database-redis
└── 📦 isass-framework-database-mybatisplus
```

### 3.3 数据库功能增强
- [ ] 优化mybatis-plus的初始化配置方式，复用mybatis-plus的配置类 【工作量：2人日】
- [ ] repository支持"有些数据库有，另一些数据库没有的sql语法"的业务查询兼容模式 【工作量：4人日】
- [ ] 实现CTE递归查询，支持层级数据查询。如果数据库支持 sql 标准的 cte 递归语法，则直接使用 cte 查询，否则在 repository 定义通用的实现方式。支持 CTE 的数据库有 Teradata, DB2, Firebird, Microsoft SQL Server, Oracle (with recursion since 11g release 2), PostgreSQL (since 8.4), MariaDB (since 10.2), SQLite (since 3.8.3), HyperSQL and H2 (experimental), MySQL8.0. 【工作量：6人日】
- [ ] 生成领域模型时，新增层级字段：level（层级）、id_hierarchical_path（id层级路径）、xxx_hierarchical_path（自定义字段层级路径） 【工作量：3人日】
- [ ] 判断数据库版本，是否支持某些特性，在repository中选择不同的实现方式 【工作量：3人日】

**CTE递归查询示例：**
```sql
-- 定义一个名为org_structure的递归公共表表达式(CTE)，用于构建组织结构层次
WITH RECURSIVE org_structure AS(
  --初始化:选取公司最高级别(CEO)的员工信息
  SELECT emp_id, emp_name, manager_id, 1 AS level, CONCAT('',emp_id) AS path
  FROM employees
  WHERE emp_name = 'CEO'
  
  UNION ALL
  -- 递归部分: 通过JOIN操作连接employees表和已生成的org_structure表，获取下一级别的员工信息
  SELECT e.emp_id, e.emp_name, e.manager_id, os.level + 1, CONCAT(os.path, ',', e.emp_id)
  FROM employees e
  JOIN org_structure os ON e.manager_id = os.emp_id
)
-- 最终查询结果:从org_structure CTE中选择需要展示的字段，并按照层级(level)和员工ID(emp_id)排序
SELECT emp_id, emp_name, manager_id, level, path
FROM org_structure
ORDER BY level, emp_id;
```

## 4. 异常处理重构

### 4.1 异常码规范
**新格式：** `5位模块号/端口号 +4位序号`

**优势：**
- 统一的异常码管理
- 开发环境便于调试
- 生产环境信息简洁

**示例：**
- 用户模块（10110 001001-10019999）
- 订单模块（10210 002001-10029999）
- 微服务端口（80808 08001-80809999）

### 4.2 响应对象优化
```java
public class Resp<T> {
    private String code;           // 异常码
    private String message;        // 简单提示信息（前端显示）
    private String detailMessage;  // 详细错误信息（开发环境）
    private T data;               // 响应数据
}
```

### 4.3 异常处理功能
- [ ] 重构异常模块，优化异常抛出的接口使用方式 【工作量：3人日】
- [ ] 异常码按微服务端口分类，isass框架的异常码按模块分类 【工作量：2人日】
- [ ] 支持捕获已有异常转换成约定的异常码和异常消息 【工作量：2人日】

## 5. 零代码模块重构

### 5.1 模块重命名
- [x] 已完成：`lowcode` → `nocode`

### 5.2 底层设计重构
- [ ] 新增低代码子模块，并把迁移v1、v2迁移进来；新增v3接口，结合DDD重新设计实现 【工作量：15人日】
- [ ] 可在数据库字段增加注释，描述各表/字段之间的关系，便于框架分析生成领域对象 【工作量：4人日】
- [ ] 支持自定义实体(非自动生成的实体)继承和实现v3系列接口 【工作量：3人日】
- [ ] 优化分页对象的选用，使用自定义分页对象或spring的分页对象 【工作量：2人日】
- [ ] 取消db实体，探索如何使用一个orm无关的实体，也能让orm功能生效。可能需要使用 lombok 自定义注解或者 javassist 动态修改源码 【工作量：8人日】
- [ ] 支持同时使用多个orm框架，支持快速切换通用代码的orm框架 【工作量：5人日】
- [ ] criteria类删除or、NotEqual等大量条件判断字段，只保留原始字段，以便加快编译速度和在idea打开java文件的速度。使用map接收并实现对应的判断逻辑 【工作量：4人日】
- [ ] 新增access接入层，支持spring的controller、socketio消息监听器、kafka消息消费者、定时任务等。各个接入实现方式使用 IService 提供的信息动态生成，无需生成代码 【工作量：10人日】
- [ ] v3通用controller使用spring接口动态生成，不再生成controller代码 【工作量：6人日】
  [ ] 只使用1个 controller,实体的路径参数可以使用枚举类，在 api 文档中可以下拉选择 【工作量：3人日】
- [ ] 新增v3代码生成器 【工作量：8人日】

### 5.3 接口设计优化
**当前问题：** 每个表生成一套完整接口，导致API文档臃肿

**新方案：**
```
📁 nocode/
├── 📦 isass-framework-nocode-core    # 核心功能
├── 📦 isass-framework-nocode-generator # 代码生成器
├── 📦 isass-framework-nocode-api     # 统一API接口
└── 📦 isass-framework-nocode-starter # 启动器
```

**优化内容：**
- 统一CRUD接口，不再为每个表生成独立接口
- 只生成实体类，直接覆盖到代码目录
- 减少API文档复杂度，提升开发效率

### 5.4 功能增强
- [ ] 基于jsr303规范，新增实体字段校验功能，支持校验功能分组 【工作量：3人日】
- [ ] 并优化spring默认返回的"校验不通过"的响应消息 【工作量：1人日】
- [ ] service逻辑提供事件监听功能，使业务能够添加运行service逻辑时的前置和后置逻辑 【工作量：4人日】
- [ ] 新增级联删除，关联表删除功能。通过接口参数判断是否需要级联/关联删除 【工作量：5人日】
- [ ] 通用的新增接口，支持配置指定字段自动赋值当前时间的功能 【工作量：2人日】
- [ ] 创建时间、修改时间字段改回bigint，java为Long，在大批量查询业务时，无需转换时间对象，以便提高性能，适应大数据处理项目。 【工作量：2人日】
- [ ] Entity 接口添加 formatTimestamp(Function gettingMapper) 、setupTimestamp(String dateTime, Function settingMapper)方法，方便在调试阶段查看和设置 Long 类型字段的日期时间。使用方式：entity.formatTimestamp(User::getCreateTime)、entity.setupTimestamp("2022-01-01 12:00:00", User::setCreateTime) 【工作量：2人日】
- [ ] 优化前端传递的查询条件是空字符串时，在最终的sql执行上，需要支持用户是需要查询字段为空的情况，还是不需要过滤字段的情况。例如用户传递 /auth-service/user?username=&age=10 时，是需要查询 username 为空的情况，还是不需要过滤 username 字段 【工作量：3人日】
- [ ] 查询接口新增支持主从表关联查询(一对一和一对多) 【工作量：6人日】
- [ ] 新增增删改一体的接口，方便前端一次性调用接口可以增删改数据。例如在多选框场景，用户可能新增勾选数据和取消勾选之前已经选择的数据 【工作量：4人日】

### 5.5 查询条件增强
- [ ] criteria查询条件支持分组，支持复杂的查询条件组合，例如：(a=1 and b=2) or (c=3 and d=4) 【工作量：5人日】

## 6. 接口文档重构

### 6.1 当前问题
- Swagger需要大量注解，代码侵入性强
- 影响代码可读性，增加维护成本

### 6.2 重构方案
**替换为Smart-Doc：**
- 基于JavaDoc注释，无代码侵入
- 生成OpenAPI格式文档
- 支持多种UI：Smart-Doc自带UI、Knife4j、zyplayer-doc
- 推荐集成zyplayer-doc（功能最全面）

### 6.3 文档系统建设
- [ ] 使用vuepress-theme-vdoing重写isass文档项目，支持多级目录 【工作量：5人日】
- [ ] 文档项目只做文档框架，文档内容自动同步isass等具体项目的md文档 【工作量：3人日】
- [ ] 集成数据库文档生成工具：screw 【工作量：2人日】

## 7. 构建和部署优化

### 7.1 构建优化
- [ ] 打通GraalVM技术路线，集成GraalVM编译，支持生成原生镜像 【工作量：8人日】
- [ ] 研究对比springboot3提供的docker集成方式，利用好docker分层技术，使docker镜像在集群环境中拉取时体积更小，选择使用原生的 dockerfile 还是 springboot3 提供的 docker 集成方式 【工作量：5人日】
- [ ] 取消fat jar的构建包方式，改为lib外置 【工作量：5人日】

### 7.2 配置文件优化
- [ ] 配置文件统一改为toml格式，合并原来的yml和properties文件 【工作量：2人日】
- [ ] toml文件放到resources/config目录 【工作量：1人日】

### 参考链接
- [toml官网](https://toml.io/cn)
- [聊一聊TOML](https://zhuanlan.zhihu.com/p/31306361)

## 8. 其他优化

### 8.1 许可证协议
- [ ] 回顾常用开源许可证协议，将isass在用的协议修改为更宽松的协议 【工作量：1人日】

## 工作量与时间安排
- **2025年下半年**：基础环境升级(22) + IOC依赖注入重构(20) + 数据库管理重构(18) + 异常处理重构(7) = **67人日**
- **2026年上半年**：零代码模块重构(75) + 接口文档重构(10) = **85人日**  
- **2026年下半年**：构建优化(21) + 其他优化(1) = **22人日**

**总工作量：174人日**
