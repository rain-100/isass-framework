# NoCode 零代码 MyBatis-Plus 代码生成器使用示例

业务服务可以在 `service` 模块的 `src/test/java` 下放置一个本地执行的代码生成器类，用于从数据库表结构生成 NoCode 零代码 相关代码。

以任意服务为例，推荐在 `{service}-service` 模块放置：

```text
{service}-service/src/test/java/vip/isass/{service}/generator/{Service}ContextMybatisPlusGenerator.java
```

生成器会同时写入 `api` 模块和 `service` 模块：

```text
{context}/domain/{domain}[/{subdomain}]/domain/model/entity/Xxx.java
{context}/domain/{domain}[/{subdomain}]/domain/model/criteria/XxxCriteria.java
{context}/application/{domain}[/{subdomain}]/service/IXxxService.java
{context}/domain/{domain}[/{subdomain}]/infrastructure/persistence/mybatisplus/XxxMapper.java
{context}/domain/{domain}[/{subdomain}]/infrastructure/persistence/mybatisplus/xml/XxxMapper.xml
{context}/domain/{domain}[/{subdomain}]/infrastructure/persistence/mybatisplus/XxxRepository.java
{context}/application/{domain}[/{subdomain}]/service/XxxService.java
```

生成的本地 NoCode CRUD 实现统一命名为 `${Entity}Service`；`ApplicationService` 仅用于手写的业务用例编排服务。
存在 `parent_id` 的实体除基础 `ICrudService` 外自动组合 `ITreeQueryService`，本地实现使用
`ILocalTreeQueryService`，并获得标准 `tree` 与 `descendantIds` 查询；普通实体不生成该能力。
生成器按表名的 `{service}_{context}_{entity}` 三段规则解析限界上下文，并从表级注释
`[--domain:{domain}]` 或 `[--domain:{domain};--subdomain:{subdomain}]` 解析领域及可选一级子域；`entity` 可以包含下划线。
子域只影响 Java 包路径，不进入物理表名；未声明子域时保持原有目录结构。

生成 Entity 的类体统一按“静态常量、数据库字段、关联字段、内部枚举、集中 setter、主键适配方法、
`associations()`、`tableName()`、`randomEntity()`”排列。setter 必须集中在全部字段声明之后，不能穿插在字段之间；
生成 Entity 不包含仅用于打印随机对象的 `main()`。数据库字段注释写入生成源码的 Javadoc 时会进行 HTML 转义，
避免 `[javaType--Map<String, Object>]` 等合法 DSL 内容被 Javadoc 当作未闭合的 HTML 标签；`ApiDoc` 说明仍保留原始文本。

## 示例

```java
package vip.isass.bsp.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import vip.isass.bsp.ServiceInfo;
import vip.isass.framework.nocode.generator.MybatisPlusGeneratorMeta;
import vip.isass.framework.nocode.generator.MybatisPlusGenerator;

import java.lang.invoke.MethodHandles;

public class AttachmentMybatisPlusGenerator {

    public static void main(String[] args) throws Exception {
        String path = MethodHandles.lookup().lookupClass().getResource("/").getPath();
        path = path.replace("target/test-classes/", "");
        String serviceOutputDir = path;
        String apiOutputDir = path.replace("bsp-service/", "bsp-api/");

        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
                .setApiOutputDir(apiOutputDir)
                .setServiceOutputDir(serviceOutputDir)
                .setDbType(DbType.MYSQL)
                .setDataSourceUrl("jdbc:mysql://127.0.0.1:3306/bsp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
                .setDataSourceUserName("root")
                .setDataSourcePassword("your-password")
                .setTablePrefix(new String[]{"bsp_"})
                .setPackageName("vip.isass")
                .setContext("bsp")
                .setExcludeTables(new String[]{
                        "(?i)(.*_)?DATABASECHANGELOG",
                        "(?i)(.*_)?DATABASECHANGELOGLOCK"
                });

        // 只生成指定业务表时打开：
        // meta.setIncludeTables(new String[]{
        //         "bsp_file_icon",
        //         "bsp_file_icon_group"
        // });

        MybatisPlusGenerator.generate(meta);
    }
}
```

## 关键配置说明

| 配置 | 说明 |
| --- | --- |
| `apiOutputDir` | API 模块根目录，生成实体、Criteria、`IXxxService` |
| `serviceOutputDir` | Service 模块根目录，生成 Mapper、Repository、本地 Service 实现 |
| `tablePrefix` | 服务表前缀，例如 `bsp_`，生成器据此继续解析 `context_entity` |
| `packageName` | 根包名，例如 `vip.isass` |
| `context` | 服务根包名，例如 `bsp`；限界上下文从表名解析，领域模型最终位于 `vip.isass.bsp.file.domain.attachment...`，CRUD Service 位于 `vip.isass.bsp.file.application.attachment...` |
| `domain` / `subdomain` | 不能单独配置，由表注释标记确定，例如 `[--domain:authorization;--subdomain:role]` |
| `includeTables` | 只生成指定表，适合局部重新生成 |
| `excludeTables` | 排除不参与生成的表 |

## Liquibase 管理表排除规则

Liquibase 管理表不能参与业务代码生成。

框架的 Liquibase 表名会按服务名自动加前缀：

```text
attachment_DATABASECHANGELOG
attachment_DATABASECHANGELOGLOCK
auth_DATABASECHANGELOG
auth_DATABASECHANGELOGLOCK
```

同一个数据库里可能同时包含单体模式或依赖服务的 Liquibase 管理表，所以不要只排除当前服务名，也不要只写无前缀表名。

推荐写法：

```java
.setExcludeTables(new String[]{
        "(?i)(.*_)?DATABASECHANGELOG",
        "(?i)(.*_)?DATABASECHANGELOGLOCK"
})
```

含义：

- `(?i)`：忽略大小写；
- `(.*_)?`：兼容任意服务名前缀，也兼容无前缀表名；
- 只匹配 Liquibase 标准 history/lock 表，避免误排除普通业务表。

## 覆盖策略

`MybatisPlusGeneratorMeta` 支持按文件类型控制是否覆盖。默认策略：

| 文件类型 | 默认覆盖 |
| --- | --- |
| Entity | 是 |
| Criteria | 是 |
| Mapper Java | 否 |
| Mapper XML | 否 |
| Repository | 否 |
| Service 接口 | 否 |
| 本地 Service 实现 | 否 |

数据库新增字段后，Entity 和 Criteria 会默认重新生成。若需要保留其中的手写修改，可以显式关闭：

```java
meta.setEntityFileOverride(false)
    .setCriteriaFileOverride(false);
```

生成器不生成 `XxxController`。标准零代码接口和 `IXxxService` 契约方法由统一动态 Adapter 暴露；
业务需要手写 Spring MVC 接口时，按限界上下文放在 `{context}/interfaces/rest`。

Mapper、Repository 和 Service 默认保留已有文件；如果需要按最新模板重建，可显式打开对应的 `*FileOverride`。

Service 接口和本地实现与 Entity、Criteria 的维护方式不同：Entity、Criteria 是可重复覆盖的数据库投影；
Service 是“生成骨架＋以该实体或聚合为业务主体的手写应用能力扩展点”，不是只能调用单一 Repository 的
CRUD 实现。只要用例的业务归属明确属于该实体或聚合，就应优先扩展 `${Entity}Service`；实现可以协调同一限界
上下文内其他聚合或领域的 Repository、应用服务和端口，也可以通过公开契约或事件参与跨上下文协作。只有用例
没有明确的实体或聚合主体，或者本身是独立、稳定的跨领域能力时，才新建 application capability。是否能够跨聚合
不是拆分 Service 的判据。仍应优先复用 NoCode；能够由标准 CRUD、Criteria、关联能力和生命周期完整表达的操作，
不得增加同义入口。业务服务的日常生成器配置不得长期打开 `serviceInterfaceFileOverride` 或
`localServiceFileOverride`，以免静默删除手写业务代码。
