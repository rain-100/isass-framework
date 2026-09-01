# 内部微服务 HMAC 认证

## 适用范围

内部 HMAC 用于 ISASS 微服务之间的 Entrypoint HTTP 调用。它只证明请求来自受信任的内部服务，不能替代用户
JWT 或外部应用 API Key：

- 用户请求触发跨服务调用时，转发原 JWT，并同时添加内部 HMAC。
- 外部应用请求触发跨服务调用时，转发原 API Key，并同时添加内部 HMAC。
- 定时任务、启动初始化等没有外部主体的调用，只添加内部 HMAC。

`CurrentPrincipalUtil.getPrincipal()` 始终返回 JWT/API Key 得到的业务主体；
`CurrentPrincipalUtil.getInternalServicePrincipal()` 独立返回 HMAC 得到的内部服务主体。两者可以同时存在。

## 配置

所有互相信任的服务配置同一组当前内部密钥。内部 Entrypoint 的 URL 仍使用
`isass.entrypoint.http.services.<serviceName>.url` 或服务发现配置：

```yaml
isass:
  security:
    internal:
      hmac-key-id: ${ISASS_INTERNAL_HMAC_KEY_ID}
      hmac-secret: ${ISASS_INTERNAL_HMAC_SECRET}
      allowed-clock-skew: 5m
```

轮换期间，接收方可在 `trusted-keys` 中临时保留旧 keyId/secret；全部调用方切换完成后删除旧密钥。
密钥只配置认证材料，不在配置文件维护服务名单或可访问 URL。

框架对内部 Entrypoint 出站请求自动添加服务名、keyId、毫秒时间戳、requestId、请求体摘要和 HMAC-SHA256
签名。接收端校验完整请求方法、路径、排序后的 Query、请求体摘要和允许的时间偏差；不依赖 Redis，也不保存
nonce。文件表单使用 `UNSIGNED-PAYLOAD`，因此必须使用受信任内网并优先启用 TLS。

## 用 Java Provider 开放内部入口

目标微服务默认不接受 HMAC 访问任何业务入口。需要内部调用的 operation 必须由 Spring Bean 显式声明：

```java
@Component
public final class BspInternalAccessProvider implements InternalAccessProvider {
    @Override
    public void defineInternalAccess(InternalAccessBuilder access) {
        access.allow(IAuthBootstrapService.class,
                        service -> service.register(null))
                .allow(IParameterService.class,
                        service -> service.getCodeValuesByKey(null));
    }
}
```

Builder 通过方法引用定位 operation，再从当前进程统一的 Entrypoint 元数据注册表取得 operationKey、HTTP Method
与最终 URL。路由以 operation 自身的分类为准：同一个 `ICrudService` 中继承的标准 CRUD 使用 `/nocode`，自行声明的
业务 operation 不使用 `/nocode`。禁止根据接口是否继承 `ICrudService` 整体推断路由，也禁止使用配置文件或手写 URL
列表维护内部访问范围。规则不限制调用方服务名：新增微服务无需通知目标服务增加调用方名单，但仍只能访问目标服务
明确开放的 operation。

没有 Entrypoint 接口的框架基础设施 Controller，可由目标服务使用
`allowRoute(operationKey, httpMethod, absolutePath)` 显式开放。该方式只适用于初始化数据等框架基础设施路由；普通业务
入口必须使用类型安全的方法引用，不能退回手写 URL。

## 授权判定

- `NONE`：仍按匿名入口处理。
- `AUTHENTICATED`：存在业务主体，或存在内部主体且 operation 已开放，即可访问。
- `ROLE`：业务主体按权限编码判断；内部主体按 Java Provider 判断。两个分支是 OR 关系。

因此，有效 JWT/API Key 即使没有目标 URL 权限，只要同一请求的内部 HMAC 有效且 operation 已开放，内部调用
仍可通过；未开放的 operation 不会因为携带 HMAC 而绕过业务权限。任何已出现但不完整、过期或签名错误的内部
HMAC 都直接返回 401，不降级为普通业务请求。HMAC 身份验证完成后的业务授权拒绝、参数校验和业务异常必须继续
交给下游统一异常处理，HMAC 过滤器不得将其改写为 401。
