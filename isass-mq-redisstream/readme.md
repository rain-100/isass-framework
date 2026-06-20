## Redis Stream MQ

`isass-mq-redisstream` 是基于 Redisson 的 Redis Stream MQ 源实现。

```yaml
isass:
  mq:
    enabled: true
    primary: redis
    sources:
      redis:
        enabled: true
        type: redisstream
        options:
          consumer-group: attachment-service
          consumer-name: attachment-1
          batch-size: 10
          poll-timeout-millis: 1000
```

业务发送和消费代码只依赖 `isass-mq-core`。
