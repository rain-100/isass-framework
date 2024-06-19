 # kernel-net-core
 
``` yaml
kernel:
  net:
    # 网络模块公共配置
    # 网络通信模块总开关
    enabled: true
  ` # 默认通信协议：tcp(已失效)、websocket(已实现)、socketio(已实现)、mqtt(未实现)
    defaultProtocol: socketio
  
    # 是否使用网关进行网络通信代理
    proxy:
      enabled: true
    
    # 使用网关代理模式时，一致性 hash 算法的参数
    consistentHash:
      totalNodeAbove: 1000
      virtualNodeCount: -1
    
    # 网络通信 socketio 协议实现
    socketio:
      enabled: true
      # 监听的 hostname
      hostname: 0.0.0.0
      # 监听的端口
      port: 20041
      
      # 服务元数据
      metadata:
        # 外网ip,不填则自动获取nacos上注册的 ip
        externalIp=192.168.1.2
        netExternalPort=${kernel.net.socketio.port}
        # 外网 url，非代理模式下暴露给客户端连接的 url。如果不填，则自动生成为：http://{netExternalIp}:{netExternalPort}
        netExternalUrl: http://127.0.0.1:20041
```