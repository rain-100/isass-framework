package vip.isass.framework.mq.kafka011;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;
import vip.isass.framework.mq.kafka011.producer.Kafka011MqProducer;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class Kafka011MqFactoryTest {

    @Test
    void createsKafkaProducerForSourceProperties() {
        Kafka011SourceProperties properties = kafkaSource();
        Kafka011MqFactory factory = new Kafka011MqFactory();

        IMqProducer producer = factory.createMqProducer(properties);

        assertThat(producer).isInstanceOf(Kafka011MqProducer.class);
    }

    @Test
    void buildsKafkaProducerPropertiesFromSource() {
        Kafka011SourceProperties properties = kafkaSource();

        Properties kafkaProperties = Kafka011MqProducer.createProperties(properties);

        assertThat(kafkaProperties)
                .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                .containsEntry(ProducerConfig.CLIENT_ID_CONFIG, "audit-producer")
                .containsEntry("acks", "all");
    }

    @Test
    void factoryAcceptsHandlersWithoutStartingWhenNoHandlersExist() {
        Kafka011MqFactory factory = new Kafka011MqFactory();

        factory.createMqConsumer(kafkaSource(), List.<IMqMessageHandler>of());

        assertThat(factory.getPropertiesType()).isEqualTo(Kafka011SourceProperties.class);
    }

    private static Kafka011SourceProperties kafkaSource() {
        Kafka011SourceProperties properties = new Kafka011SourceProperties();
        properties.setName("audit");
        properties.setEnabled(true);
        properties.setServers("localhost:9092");
        properties.setProducerId("audit-producer");
        properties.setProperties(Map.of("acks", "all"));
        return properties;
    }
}
