// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

import org.junit.jupiter.api.Test;
import vip.isass.framework.mq.core.config.DynamicMqProperties;
import vip.isass.framework.mq.core.config.MqSourceProperties;
import vip.isass.framework.mq.core.consumer.IMqConsumerContainer;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.producer.IMqProducer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqManagerTest {

    @Test
    void startsOnlyEnabledSourcesAndRoutesMessageToPrimarySource() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of(
                "master", source("master", true, "recording"),
                "audit", source("audit", false, "recording")
        ));
        RecordingFactory.reset();

        MqManager manager = new MqManager(properties, List.of(), List.of(new RecordingFactory()));
        manager.start();

        MqPublisher.send(new MqMessage().setTopic("order").setPayload("created"));

        assertThat(RecordingFactory.createdSources()).containsExactly("master");
        assertThat(RecordingFactory.producer("master").messages()).hasSize(1);
    }

    @Test
    void routesMessageToExplicitSource() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of(
                "master", source("master", true, "recording"),
                "audit", source("audit", true, "recording")
        ));
        RecordingFactory.reset();

        MqManager manager = new MqManager(properties, List.of(), List.of(new RecordingFactory()));
        manager.start();

        MqPublisher.send("audit", new MqMessage().setTopic("audit").setPayload("logged"));

        assertThat(RecordingFactory.producer("master").messages()).isEmpty();
        assertThat(RecordingFactory.producer("audit").messages()).hasSize(1);
    }

    @Test
    void failsWhenSendingToUnknownSource() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of("master", source("master", true, "recording")));
        RecordingFactory.reset();

        MqManager manager = new MqManager(properties, List.of(), List.of(new RecordingFactory()));
        manager.start();

        assertThatThrownBy(() -> MqPublisher.send("missing", new MqMessage()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void passesOnlyMatchingHandlersToSourceFactory() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of("master", source("master", true, "recording")));
        RecordingFactory.reset();

        IMqMessageHandler masterHandler = handler("master");
        IMqMessageHandler auditHandler = handler("audit");
        MqManager manager = new MqManager(properties, List.of(masterHandler, auditHandler), List.of(new RecordingFactory()));
        manager.start();

        assertThat(RecordingFactory.handlers("master")).containsExactly(masterHandler);
    }

    @Test
    void failsWhenSourceTypeIsMissing() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of("master", source("master", true)));
        RecordingFactory.reset();

        MqManager manager = new MqManager(properties, List.of(), List.of(new RecordingFactory()));
        assertThatThrownBy(manager::start)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void stopDestroysCreatedConsumersAndProducers() {
        DynamicMqProperties properties = new DynamicMqProperties();
        properties.setEnabled(true);
        properties.setPrimary("master");
        properties.setSources(Map.of("master", source("master", true, "recording")));
        RecordingFactory.reset();

        MqManager manager = new MqManager(properties, List.of(handler("master")), List.of(new RecordingFactory()));
        manager.start();
        manager.stop();

        assertThat(RecordingFactory.consumer("master").destroyed()).isTrue();
        assertThat(RecordingFactory.producer("master").destroyed()).isTrue();
    }

    private static TestSourceProperties source(String name, boolean enabled) {
        TestSourceProperties properties = new TestSourceProperties();
        properties.setName(name);
        properties.setEnabled(enabled);
        return properties;
    }

    private static TestSourceProperties source(String name, boolean enabled, String type) {
        TestSourceProperties properties = new TestSourceProperties();
        properties.setName(name);
        properties.setEnabled(enabled);
        properties.setType(type);
        return properties;
    }

    private static IMqMessageHandler handler(String source) {
        return new IMqMessageHandler() {
            @Override
            public String getSource() {
                return source;
            }

            @Override
            public String getTopic() {
                return "topic";
            }

            @Override
            public void consume(MqMessage mqMessage) {
            }
        };
    }

    static class TestSourceProperties extends MqSourceProperties {
    }

    public static class RecordingFactory implements IMqFactory {

        private static final List<String> CREATED_SOURCES = new ArrayList<>();
        private static final Map<String, RecordingProducer> PRODUCERS = new java.util.LinkedHashMap<>();
        private static final Map<String, RecordingConsumer> CONSUMERS = new java.util.LinkedHashMap<>();
        private static final Map<String, List<IMqMessageHandler>> HANDLERS = new java.util.LinkedHashMap<>();

        static void reset() {
            CREATED_SOURCES.clear();
            PRODUCERS.clear();
            CONSUMERS.clear();
            HANDLERS.clear();
        }

        static List<String> createdSources() {
            return CREATED_SOURCES;
        }

        static RecordingProducer producer(String source) {
            return PRODUCERS.get(source);
        }

        static RecordingConsumer consumer(String source) {
            return CONSUMERS.get(source);
        }

        static List<IMqMessageHandler> handlers(String source) {
            return HANDLERS.get(source);
        }

        @Override
        public String getType() {
            return "recording";
        }

        @Override
        public Class<? extends MqSourceProperties> getPropertiesType() {
            return TestSourceProperties.class;
        }

        @Override
        public IMqConsumerContainer createMqConsumer(MqSourceProperties sourceProperties,
                                                     List<IMqMessageHandler> mqMessageHandlers) {
            HANDLERS.put(sourceProperties.getName(), List.copyOf(mqMessageHandlers));
            RecordingConsumer consumer = new RecordingConsumer();
            CONSUMERS.put(sourceProperties.getName(), consumer);
            return consumer;
        }

        @Override
        public IMqProducer createMqProducer(MqSourceProperties sourceProperties) {
            CREATED_SOURCES.add(sourceProperties.getName());
            RecordingProducer producer = new RecordingProducer();
            PRODUCERS.put(sourceProperties.getName(), producer);
            return producer;
        }
    }

    static class RecordingProducer implements IMqProducer {

        private final List<MqMessage> messages = new ArrayList<>();

        private boolean destroyed;

        List<MqMessage> messages() {
            return messages;
        }

        boolean destroyed() {
            return destroyed;
        }

        @Override
        public void init() {
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public void send(MqMessage mqMessage) {
            messages.add(mqMessage);
        }
    }

    static class RecordingConsumer implements IMqConsumerContainer {

        private boolean destroyed;

        boolean destroyed() {
            return destroyed;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }
    }
}
