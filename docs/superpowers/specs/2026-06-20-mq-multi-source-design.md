# MQ Multi Source Design

## Goal

Refactor the v4 MQ modules into a source-oriented abstraction similar to database multi-source support. Business code depends only on `isass-mq-core`; projects choose Spring Event, Kafka, or future MQ products through dependencies and configuration.

## Scope

This is a breaking v4 change. Compatibility with the v3 `manufacturer` model is not required.

The work covers:

- `isass-mq-core`: common message model, source properties, factory abstraction, runtime manager, publisher, and consumer handler contracts.
- `isass-mq-springevent`: in-process MQ source implementation used for lightweight/default deployments and tests.
- `isass-mq-kafka011`: Kafka 0.11 source implementation adapted to the new factory/source model.
- Documentation and changelog updates.

## Design

`DynamicMqProperties` is the root configuration object. It owns a global `enabled` flag, a `primary` source name, and a map of named `MqSourceProperties`. Each named source declares whether it is enabled and which `IMqFactory` creates its producer and consumer.

`MqManager` owns runtime state. On startup it filters enabled sources, asks each source factory to create an `IMqProducer`, and asks each factory to start consumers for the handlers matching that source. Producers are stored by source name. Sending without a source uses the configured primary source.

Business publishing uses `MqPublisher.send(MqMessage)` or `MqPublisher.send(String source, MqMessage)`. Business consumers implement `IMqMessageHandler`; the handler declares `source`, `topic`, `tag`, retry behavior, and consume logic. This keeps business code independent from Kafka, Spring Event, RocketMQ, or other concrete products.

## Configuration Shape

The intended property shape is:

```yaml
isass:
  mq:
    enabled: true
    primary: master
    sources:
      master:
        enabled: true
        factory-class: vip.isass.framework.mq.spring.event.SpringEventMqFactory
      audit:
        enabled: true
        factory-class: vip.isass.framework.mq.kafka011.Kafka011MqFactory
        options:
          servers: localhost:9092
          consumer-group-id: audit-service
```

The source map can hold product-specific subclasses of `MqSourceProperties`.

## Error Handling

Startup fails fast when MQ is enabled but the primary source is missing, a source has no factory class, or no producer exists for a requested source. Disabled global MQ skips initialization. Disabled sources are ignored.

Consumer exceptions use `FailStrategy`: ignore, retry by backend policy, or retry immediately in-process for a configured count.

## Testing

Core tests cover source registration, primary source fallback, explicit source routing, disabled source filtering, and missing source failures.

Spring Event tests cover handler selection by source/topic/tag and publishing through the Spring application event publisher abstraction.

Kafka tests focus on configuration/property assembly and factory wiring without requiring a live Kafka broker.
