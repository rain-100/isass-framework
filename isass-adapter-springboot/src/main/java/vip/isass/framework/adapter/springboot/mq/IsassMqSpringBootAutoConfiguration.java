package vip.isass.framework.adapter.springboot.mq;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.adapter.springboot.condition.ConditionalOnIsassFeature;
import vip.isass.framework.adapter.springboot.condition.IsassFeature;
import vip.isass.framework.mq.core.IMqFactory;
import vip.isass.framework.mq.core.MqManager;
import vip.isass.framework.mq.core.config.DynamicMqProperties;
import vip.isass.framework.mq.core.consumer.IMqMessageHandler;
import vip.isass.framework.mq.core.consumer.MqConsumerAutoConfiguration;
import vip.isass.framework.mq.core.consumer.MqConsumerManager;
import vip.isass.framework.mq.core.producer.EventPublisher;
import vip.isass.framework.mq.core.producer.ProducerManager;

import java.util.function.BooleanSupplier;

@AutoConfiguration
@ConditionalOnIsassFeature(IsassFeature.MQ_CORE)
@EnableConfigurationProperties
public class IsassMqSpringBootAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "isass.mq")
    public DynamicMqProperties dynamicMqProperties() {
        return new DynamicMqProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public MqManager mqManager(DynamicMqProperties dynamicMqProperties,
                               ObjectProvider<IMqMessageHandler> mqMessageHandlers,
                               ObjectProvider<IMqFactory> mqFactories) {
        return new MqManager(
                dynamicMqProperties,
                mqMessageHandlers.orderedStream().toList(),
                mqFactories.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public EventPublisher eventPublisher(DynamicMqProperties dynamicMqProperties,
                                         ObjectProvider<ProducerManager> producerManagers) {
        return new EventPublisher(dynamicMqProperties, producerManagers.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public MqConsumerAutoConfiguration mqConsumerAutoConfiguration(
            DynamicMqProperties dynamicMqProperties,
            ObjectProvider<MqConsumerManager> mqConsumerManagers) {
        return new MqConsumerAutoConfiguration(dynamicMqProperties, mqConsumerManagers.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean(name = "mqManagerLifecycle")
    public SmartLifecycle mqManagerLifecycle(MqManager mqManager) {
        return new DelegatingSmartLifecycle(mqManager::start, mqManager::stop, mqManager::isRunning);
    }

    @Bean
    @ConditionalOnMissingBean(name = "eventPublisherLifecycle")
    public SmartLifecycle eventPublisherLifecycle(EventPublisher eventPublisher) {
        return new DelegatingSmartLifecycle(eventPublisher::start, eventPublisher::stop, eventPublisher::isRunning);
    }

    @Bean
    @ConditionalOnMissingBean(name = "mqConsumerLifecycle")
    public SmartLifecycle mqConsumerLifecycle(MqConsumerAutoConfiguration mqConsumerAutoConfiguration) {
        return new DelegatingSmartLifecycle(
                mqConsumerAutoConfiguration::start,
                mqConsumerAutoConfiguration::stop,
                mqConsumerAutoConfiguration::isRunning);
    }

    private static class DelegatingSmartLifecycle implements SmartLifecycle {

        private final Runnable start;

        private final Runnable stop;

        private final BooleanSupplier running;

        private DelegatingSmartLifecycle(Runnable start, Runnable stop, BooleanSupplier running) {
            this.start = start;
            this.stop = stop;
            this.running = running;
        }

        @Override
        public void start() {
            start.run();
        }

        @Override
        public void stop() {
            stop.run();
        }

        @Override
        public boolean isRunning() {
            return running.getAsBoolean();
        }
    }
}
