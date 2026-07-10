# NoCode V3 MyBatis-Plus 代码生成器使用示例

业务微服务可以在 `service` 模块的 `src/test/java` 下放置一个本地执行的代码生成器类，用于从数据库表结构生成 NoCode V3 相关代码。

以 attachment 服务为例，推荐位置：

```text
isass-service-attachment-service/src/test/java/vip/isass/attachment/generator/V3AttachmentMybatisPlusGenerator.java
```

生成器会同时写入 `api` 模块和 `service` 模块：

```text
api/model/entity/V3Xxx.java
api/model/criteria/V3XxxCriteria.java
api/service/IV3XxxService.java
db/mapper/V3XxxMapper.java
db/mapper/xml/V3XxxMapper.xml
db/repository/V3XxxRepository.java
service/V3XxxService.java
```

## 示例

```java
package vip.isass.attachment.generator;

import com.baomidou.mybatisplus.annotation.DbType;
import vip.isass.framework.nocode.v3.generator.MybatisPlusGeneratorMeta;
import vip.isass.framework.nocode.v3.generator.V3MybatisPlusGenerator;

import java.lang.invoke.MethodHandles;

public class V3AttachmentMybatisPlusGenerator {

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
                .setTablePrefix(new String[]{"att_"})
                .setPackageName("vip.isass")
                .setModuleName("attachment")
                .setControllerPrefix("/attachment-service")
                .setExcludeTables(new String[]{
                        "(?i)(.*_)?DATABASECHANGELOG",
                        "(?i)(.*_)?DATABASECHANGELOGLOCK"
                });

        // 只生成指定业务表时打开：
        // meta.setIncludeTables(new String[]{
        //         "att_icon",
        //         "att_icon_group"
        // });

        V3MybatisPlusGenerator.generate(meta);
    }
}
```

## 关键配置说明

| 配置 | 说明 |
| --- | --- |
| `apiOutputDir` | API 模块根目录，生成实体、Criteria、`IV3XxxService` |
| `serviceOutputDir` | Service 模块根目录，生成 Mapper、Repository、本地 Service 实现 |
| `tablePrefix` | 数据库表前缀，例如 `att_`，生成 Java 类名时会移除 |
| `packageName` | 根包名，例如 `vip.isass` |
| `moduleName` | 业务模块名，例如 `attachment`，最终包名为 `vip.isass.attachment...` |
| `controllerPrefix` | 历史兼容配置；V3 当前不生成实体 Controller |
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
| Entity | 否 |
| Criteria | 否 |
| Mapper Java | 是 |
| Mapper XML | 是 |
| Repository | 是 |
| Service 接口 | 是 |
| 本地 Service 实现 | 是 |
| Controller | 是 |

数据库新增字段后，如果需要重新生成实体或 Criteria，可以显式打开：

```java
meta.setEntityFileOverride(true)
    .setCriteriaFileOverride(true);
```

如果业务已经在某些生成文件里手写逻辑，可以把对应文件类型设置为不覆盖。
