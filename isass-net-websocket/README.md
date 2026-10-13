# isass-net-websocket

WebSocket 协议实现。将 WebSocket 连接和消息接入统一网络能力。

- 依赖 net-core；连接身份与业务消息授权由运行服务约束。
- NIO 事件循环使用 MultiThreadIoEventLoopGroup 与 NioIoHandler，避免 Netty 4.2 已弃用的 NioEventLoopGroup；通道配置不变。

[返回模块目录](../README.md)
