# NoCode 零代码 MyBatis-Plus 代码生成器使用示例

业务微服务可以在 `service` 模块的 `src/test/java` 下放置一个本地执行的代码生成器类，用于从数据库表结构生成 NoCode 零代码 相关代码。

以 attachment 服务为例，推荐位置：

```text
isass-service-attachment-service/src/test/java/vip/isass/attachment/generator/AttachmentMybatisPlusGenerator.java
```

生成器会同时写入 `api` 模块和 `service` 模块：

```text
api/model/entity/Xxx.java
api/model/criteria/XxxCriteria.java
api/service/IXxxService.java
db/mapper/XxxMapper.java
db/mapper/xml/XxxMapper.xml
db/repository/XxxRepository.java
service/XxxService.java
controller/XxxController.java
```

## 示例

```java
package vip.isass.attachment.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import vip.isass.framework.nocode.generator.MybatisPlusGeneratorMeta;
import vip.isass.framework.nocode.generator.MybatisPlusGenerator;

import java.lang.invoke.MethodHandles;

public class AttachmentMybatisPlusGenerator {

    public static void main(String[] args) throws Exception {
        String path = MethodHandles.lookup().lookupClass().getResource("/").getPath();
        path = path.replace("target/test-classes/", "");
        String serviceOutputDir = path;
        String apiOutputDir = path.replace("-service/", "-api/");

        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
                .setApiOutputDir(apiOutputDir)
                .setServiceOutputDir(serviceOutputDir)
                .setDbType(DbType.MYSQL)
                .setDataSourceUrl("jdbc:mysql://127.0.0.1:3306/attachment?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai")
                .setDataSourceUserName("root")
                .setDataSourcePassword("your-password")
                .setTablePrefix(new String[]{ModuleInfo.TABLE_PREFIX})
                .setPackageName(ModuleInfo.GROUP_ID)
                .setModuleName(ModuleInfo.SERVICE_NAME)
                .setControllerPrefix(ModuleInfo.SERVICE_URL_PREFIX)
                .setExcludeTables(new String[]{
                        "(?i)(.*_)?DATABASECHANGELOG",
                        "(?i)(.*_)?DATABASECHANGELOGLOCK"
                });

        // 只生成指定业务表时打开：
        // meta.setIncludeTables(new String[]{
        //         "att_icon",
        //         "att_icon_group"
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
| `tablePrefix` | 数据库表前缀，例如 `att_`，生成 Java 类名时会移除 |
| `packageName` | 根包名，例如 `vip.isass` |
| `moduleName` | 业务模块名，例如 `attachment`，最终包名为 `vip.isass.attachment...` |
| `controllerPrefix` | 标准零代码 动态 HTTP 路由前缀配置；生成的实体 Controller 仅作为手写 Spring MVC 扩展外壳 |
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

同一个数据库里可能同时包含单体模式或依赖微服务的 Liquibase 管理表，所以不要只排除当前服务名，也不要只写无前缀表名。

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
| Controller | 否 |

数据库新增字段后，Entity 和 Criteria 会默认重新生成。若需要保留其中的手写修改，可以显式关闭：

```java
meta.setEntityFileOverride(false)
    .setCriteriaFileOverride(false);
```

生成的 `XxxController` 不承载标准 CRUD。标准零代码接口和 `IXxxService` 契约方法由统一动态 Adapter 暴露；`XxxController` 只用于业务手写 Spring MVC 接口。

因为 Controller 常承载手写接口，默认不覆盖。如果确认要重置 Controller 外壳，可以显式打开：

```java
meta.setControllerFileOverride(true);
```

Mapper、Repository、Service 和 Controller 默认保留已有文件；如果需要按最新模板重建，可显式打开对应的 `*FileOverride`。
