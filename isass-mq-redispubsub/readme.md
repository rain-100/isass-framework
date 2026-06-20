## Redis Pub/Sub MQ

`isass-mq-redispubsub` 是基于 Redisson 的 Redis Pub/Sub MQ 源实现。

```yaml
isass:
  mq:
    enabled: true
    primary: redis
    sources:
      redis:
        enabled: true
        type: redispubsub
```

业务发送和消费代码只依赖 `isass-mq-core`。
