## MQ

v4 的 MQ 模块采用多源模型，业务代码只依赖 `isass-mq-core`，具体 MQ 产品由项目依赖和配置决定。

### 发送消息

默认发送到 `primary` 源：

```java
MqPublisher.send(new MqMessage()
        .setTopic("order")
        .setTag("created")
        .setPayload(payload));
```

指定 MQ 源：

```java
MqPublisher.send("audit", new MqMessage()
        .setTopic("audit")
        .setTag("logged")
        .setPayload(payload));
```

### 消费消息

```java
@Component
public class OrderCreatedHandler implements IMqMessageHandler {

    @Override
    public String getSource() {
        return "master";
    }

    @Override
    public String getTopic() {
        return "order";
    }

    @Override
    public String getTag() {
        return "created";
    }

    @Override
    public void consume(MqMessage mqMessage) {
        // business logic
    }
}
```

### 配置

```yaml
isass:
  mq:
    enabled: true
    primary: master
    sources:
      master:
        enabled: true
        type: spring-event
      audit:
        enabled: true
        type: kafka011
        options:
          servers: localhost:9092
          producer-id: audit-producer
          consumer-group-id: audit-service
```

每个 MQ 源必须显式配置 `type`。`type` 用于选择具体 MQ 实现，例如 `spring-event`、`kafka011`、`redisstream` 或 `redispubsub`。
