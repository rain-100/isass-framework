## Kafka 0.11 MQ Source

Kafka 0.11 实现作为 `isass-mq-core` 的一个 MQ 源使用。

```yaml
isass:
  mq:
    enabled: true
    primary: audit
    sources:
      audit:
        enabled: true
        type: kafka011
        options:
          servers: localhost:9092
          producer-id: audit-producer
          consumer-group-id: audit-service
          common-message-topic: audit-common
```

业务发送：

```java
MqPublisher.send("audit", new MqMessage()
        .setTopic("audit-common")
        .setTag("logged")
        .setPayload(payload));
```
