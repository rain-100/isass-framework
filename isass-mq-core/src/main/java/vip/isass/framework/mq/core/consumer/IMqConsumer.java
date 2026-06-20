package vip.isass.framework.mq.core.consumer;

import tools.jackson.core.type.TypeReference;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessageContext;

import java.util.Collections;
import java.util.Map;

public interface IMqConsumer {

    default String getManufacturer() {
        return "";
    }

    default String getConsumerId() {
        return getClass().getName();
    }

    String getTopic();

    default String getTag() {
        return "*";
    }

    void consume(MqMessageContext mqMessageContext);

    default Map<String, ?> getProperties() {
        return Collections.emptyMap();
    }

    default int getMessageType() {
        return MessageType.COMMON_MESSAGE;
    }

    default FailStrategy getFailStrategy() {
        return FailStrategy.RETRY;
    }

    default int getImmediatelyRetryCount() {
        return 1;
    }

    default TypeReference<?> getTypeReference() {
        return null;
    }
}
