# isass 异常码体系设计

## 一、概述

isass 框架异常码体系基于**模块前缀 + 本地偏移**的分层编码规则，每个模块独立管理自身异常码，通过 `ModuleInfo` 声明模块身份，通过 `IStatusMapping` / `IExceptionMapping` 两套接口分别处理状态码查询和异常映射。

## 二、核心接口

### 2.1 IStatusMessage（异常码载体）

```java
// vip.isass.framework.common.exception.code.IStatusMessage
public interface IStatusMessage {
    Integer getStatus();   // 异常码
    String getMsg();       // 用户提示消息
}
```

所有异常码枚举必须实现此接口。

### 2.2 IStatusMapping（状态码 → 消息）

```java
// vip.isass.framework.common.exception.IStatusMapping
public interface IStatusMapping {
    IStatusMessage getErrorCode(Integer code);
}
```

用于将 HTTP 状态码或业务异常码解析为对应的 `IStatusMessage`。由 `IsassErrorController` 调用，负责处理 Controller 层以外的错误响应。

### 2.3 IExceptionMapping（异常类型 → 状态码）

```java
// vip.isass.framework.common.exception.IExceptionMapping
public interface IExceptionMapping {
    IStatusMessage getStatusCode(Exception exception);
    String parseExceptionMessage(Throwable e);
    default String parseMessage(Throwable t, IStatusMessage statusMessage);
}
```

用于将特定异常类型映射为对应的 `IStatusMessage`。由 `ExceptionAdvice` 调用，负责处理 Controller 内部抛出的异常。

## 三、模块编码规则

### 3.1 编码格式

```
完整异常码 = MODULE_CODE × 10000 + local_code
```

- `MODULE_CODE`：5 位模块编号，框架模块使用 `hashCode`，业务微服务使用端口号
- `local_code`：模块本地异常码，范围 0~9999
- 示例：`10025 * 10000 + 1001 = 100251001`

### 3.2 ModuleInfo

每个模块必须在自己的包内定义 `ModuleInfo` 接口：

```java
// 示例：isass-security-springsecurity
public interface ModuleInfo {
    Integer MODULE_CODE = 10025;
    Integer STATUS_CODE_PREFIX = MODULE_CODE * 10000;
}
```

当前框架各模块 MODULE_CODE：

| 模块 | 取值方式 | 定义位置 |
|------|---------|---------|
| `isass-core-common` | hashCode | `vip.isass.framework.common.exception.code.ModuleInfo` |
| `isass-database-core` | hashCode | `vip.isass.framework.database.core.ModuleInfo` |
| `isass-nocode-core` | hashCode | `vip.isass.framework.nocode.core.ModuleInfo` |
| `isass-security-springsecurity` | 10025 | `vip.isass.framework.web.security.ModuleInfo` |

### 3.3 ModuleCodeResolver

```java
// vip.isass.framework.common.exception.code.ModuleCodeResolver
public final class ModuleCodeResolver {
    public static final int MODULE_PREFIX_MULTIPLIER = 10000;
    public static int resolveModuleCode(int statusCode);    // 提取模块编号
    public static int compose(int moduleCode, int localCode); // 组合完整异常码
}
```

## 四、两种 StatusMapping 模式

### 4.1 Web 模式（HTTP 标准码 → Map）

适用于 HTTP 行业规范编码（403/404/405/500 等），无需业务代码手动抛出。

```java
// isass-web-springmvc: WebStatusMapping
public class WebStatusMapping implements IStatusMapping {
    private static final Map<Integer, IStatusMessage> statusMapping = MapUtil
        .<Integer, IStatusMessage>builder()
        .put(403, StatusMessageEnum.ACCESS_DENIED_403)
        .put(404, StatusMessageEnum.NOT_FOUND_404)
        .build();

    public IStatusMessage getErrorCode(Integer code) {
        return statusMapping.get(code);
    }
}
```

### 4.2 业务模块模式（枚举 → 码）

适用于业务异常码，通过枚举定义码值。业务代码通过枚举引用抛出异常：

```java
// 示例：isass-database-core: DatabaseStatusMapping
public class DatabaseStatusMapping implements IStatusMapping {
    private static final Map<Integer, IStatusMessage> STATUS_MAPPING = Arrays
        .stream(DatabaseStatusEnum.values())
        .collect(toMap(DatabaseStatusEnum::getStatus, identity()));

    public enum DatabaseStatusEnum implements IStatusMessage {
        SQL_EXCEPTION(ModuleInfo.STATUS_CODE_PREFIX + 1001, "数据库错误"),
        TOO_MANY_RESULT(ModuleInfo.STATUS_CODE_PREFIX + 1002, "数据重复"),
        ;
        // getter + 构造器
    }
}

// 业务代码使用：
throw new UnifiedException(DatabaseStatusMapping.DatabaseStatusEnum.SQL_EXCEPTION);
```

### 4.3 DatabaseExceptionMapping（异常类 → 枚举）

数据库等模块有特定异常类（如 `SQLException`、`DuplicateKeyException`），需要通过 `IExceptionMapping` 将异常类型映射到对应枚举：

```java
// isass-database-mybatisplus: BuildInDatabaseExceptionMapping
public class BuildInDatabaseExceptionMapping implements IExceptionMapping {
    private static Map<Class<?>, IStatusMessage> MAPPING = MapUtil.builder()
        .put(DuplicateKeyException.class, DatabaseStatusMapping.DatabaseStatusEnum.DUPLICATE_KEY)
        .build();
}
```

## 五、SPI 发现机制

### 5.1 IExceptionMapping（已有）

通过 `META-INF/services/vip.isass.framework.common.exception.IExceptionMapping` 文件注册实现类。`ExceptionAdvice` 通过 `IsassServiceLoader` 加载 SPI 实现并与 Spring Bean 合并。

### 5.2 IStatusMapping（已有）

通过 `META-INF/services/vip.isass.framework.common.exception.IStatusMapping` 文件注册实现类。`IsassErrorController` 通过 `IsassServiceLoader` 加载 SPI 实现并与 Spring Bean 合并。

当前注册：

```
isass-web-springmvc → WebStatusMapping
isass-database-core → DatabaseStatusMapping
isass-nocode-core   → NocodeStatusMapping
```

## 六、新增模块 Checklist

1. 定义 `ModuleInfo`，设置 `MODULE_CODE`（hashCode 或固定值）
2. 创建 `XxxStatusMapping implements IStatusMapping`，内嵌 `XxxStatusEnum implements IStatusMessage`
3. 异常码使用 `ModuleInfo.STATUS_CODE_PREFIX + local_code`
4. 注册 SPI：`META-INF/services/vip.isass.framework.common.exception.IStatusMapping`
5. 如有模块特有异常类，创建 `XxxExceptionMapping implements IExceptionMapping`
6. 补充单元测试

## 七、StatusMessageEnum 定位

`StatusMessageEnum`（`isass-core-common`）仅保留框架通用异常码：

- 通用操作结果：`SUCCESS`、`FAIL`、`UNDEFINED`
- 通用逻辑异常：`ALREADY_PRESENT`、`ABSENT`、`UN_SUPPORT_OPERATION`、`ILLEGAL_ARGUMENT_ERROR`
- 通用 I/O 异常：`IO_ERROR`、`FILE_NOT_FOUND`、`DATE_TIME_ERROR`
- HTTP 标准码：`ACCESS_DENIED_403`、`NOT_FOUND_404`、`METHOD_NOT_ALLOWED_405`、`INTERNAL_SERVER_ERROR_500`

模块特有异常码应从 `StatusMessageEnum` 移除，迁移至各模块独立枚举。
