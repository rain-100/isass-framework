# V4 服务端口

本表只描述当前 V4 可启动的服务。端口配置以各服务 `*-boot/src/main/resources/config/application.yml` 为准；部署时可通过其对应环境变量覆盖。

| 服务                   | 默认 HTTP 端口 | 说明                                                                                                     |
|------------------------|---------------:|----------------------------------------------------------------------------------------------------------|
| `gateway-service`      |          20000 | 网关                                                                                                     |
| `message-service`      |          20160 | 短信、App 系统通知与推送；不承载即时通信                                                                 |
| `isass-service-apidoc` |          20200 | API 文档服务                                                                                             |
| `bsp-service`          |          31010 | Basic Service Platform；包含 attachment、filesystem、auth、dict、param、dir、equipment、area、log 上下文 |
| `im-service`           |          20385 | 即时通信服务；可通过 `IM_HTTP_PORT` 覆盖                                                                 |
| `wechat-service`       |          20190 | 微信小程序能力                                                                                           |
| `bsp-web-vue`          |          31011 | bsp前端管理                                                                                              |
| `iimage-service`       |          31020 | 智图资产管理服务                                                                                         |
| `iimage-asset-h5`      |          31025 | 智图资产管理前端                                                                                         |

## 不再作为 V4 独立部署服务的历史工程

`isass-service-attachment`、`isass-service-auth`、`isass-service-base`、`isass-service-log`、`isass-service-uom` 的能力已经并入
`bsp-service`。这些仓库的旧模块仅用于迁移核对，待仓库外调用方完成 V4 契约切换审计后才可物理下线；不得据此表启动或配置它们。

Socket.IO、WebSocket 协议节点和动态 gRPC Server 当前均为遗留预研项，不是本轮 V4 的部署或验收前提。

## 第三方服务

| 名称                         | 端口                               |
|------------------------------|------------------------------------|
| oap (SkyWalking)             | 11800                              |
| Jenkins                      | 30000                              |
| Elasticsearch                | 30010                              |
| Redis                        | 30020                              |
| MySQL                        | 30030                              |
| Nacos                        | 30040                              |
| PostgreSQL                   | 30060                              |
| 达梦 / KingBase 等目标数据库 | 由集成环境分配；不应假定开发机端口 |
| Prometheus                   | 30210                              |
| Alertmanager                 | 30211                              |
| Grafana                      | 30212                              |
