# 端口分配列表

## 微服务

| 名称                         | 端口                        |
|----------------------------|---------------------------|
| isass-service-gateway      | http:20000                |
| isass-service-config       | http:20010                |
| isass-service-gateway-http | http:20020                |
| isass-service-gateway-tcp  | 20030,20031               |
| isass-service-auth         | http:20040                |
| isass-service-message      | http:20050                |
| isass-service-test-tcp     | http:20060                |
| isass-service-websocket    | http:20070 ws:20071       |
| isass-service-base         | http:20080                |
| isass-service-goods        | http:20090                |
| isass-service-order        | http:20100                |
| isass-service-job-center   | http:20110                |
| isass-service-job-executor | http:20120 tcp:20121      |
| isass-service-search       | http:20130                |
| isass-service-finance      | http:20140                |
| isass-service-pay          | http:20150                |
| isass-service-taobao       | http:20160                |
| isass-service-socketio     | http:20170 socketio:20171 |
| isass-service-order        | http:20180                |
| isass-service-poster       | http:20190                |
| isass-service-apidoc       | http:20200                |
| isass-service-pdd          | http:20210                |
| isass-service-activity     | http:20220                |
| isass-service-coupon       | http:20230                |
| isass-service-push         | http:20240                |
| isass-service-isass        | http:20250                |
| isass-service-upload       | http:20260                |
| isass-service-advertising  | http:20270                |
| isass-service-etg          | http:20280                |
| isass-service-oa           | http:20290                |
| isass-service-exhibition   | http:20300                |
| isass-service-demo         | http:20310                |
| isass-service-attachment   | http:20320                |
| isass-service-log          | http:20330                |
| isass-doc                  | http:20340                |
| isass-service-tag          | http:20350                |
| isass-service-uom          | http:20360                |
| isass-service-wechat       | http:20370                |

## 第三方服务

| 名称              | 端口                                                        |
|-----------------|-----------------------------------------------------------|
| oap(skywalking) | 11800                                                     |
| oapui           | 8080                                                      |
| jenkins         | 30000                                                     |
| elasticsearch   | 30010                                                     |
| elasticvue      | 30015                                                     |
| redis           | 30020                                                     |
| redis_insight   | 30021                                                     |
| redis_exporter  | 30022                                                     |
| mysql           | 30030                                                     |
| mysql_exporter  | 30031                                                     |
| nacos           | 30040                                                     |
| rancher         | http30050,https30051                                      |
| postgresql      | 30060                                                     |
| postgis         | 30065                                                     |
| nps             | web:30070 bridge:30071 http_proxy:30072 https_proxy:30073 |
| node_exporter   | 30080                                                     |
| nexus3          | 30090                                                     |
| stun            | tcp/udp 30101                                             |
| prometheus      | 30210                                                     |
| alertManager    | 30211                                                     |
| grafana         | 30212                                                     |
| nas             | http:30220 https:30221                                    |
| zerotier-planet | http:30230 tcp/udp:30231                                  |
| hgdb            | 30240                                                     |
