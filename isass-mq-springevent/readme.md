## Spring Event MQ Source

Spring Event 实现适合单机进程内事件，不适合分布式消费。

```yaml
isass:
  mq:
    enabled: true
    primary: master
    sources:
      master:
        enabled: true
        type: spring-event
        options:
          default-topic: default
```

业务代码仍然只使用 `MqPublisher` 和 `IMqMessageHandler`，不需要依赖 Spring Event 的具体类型。
