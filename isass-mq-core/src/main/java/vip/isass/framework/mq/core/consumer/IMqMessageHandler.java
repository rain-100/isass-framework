// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.consumer;

import tools.jackson.core.type.TypeReference;
import vip.isass.framework.common.mq.MessageType;
import vip.isass.framework.mq.core.FailStrategy;
import vip.isass.framework.mq.core.MqMessage;
import vip.isass.framework.mq.core.SubscribeModel;

import java.util.Collections;
import java.util.Map;

public interface IMqMessageHandler {

    default String getSource() {
        return "";
    }

    default SubscribeModel getSubscribeModel() {
        return SubscribeModel.CLUSTERING;
    }

    default String getConsumerId() {
        return getClass().getName();
    }

    String getTopic();

    default String getTag() {
        return "*";
    }

    default Integer getConsumeThreadNumber() {
        return null;
    }

    void consume(MqMessage mqMessage);

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

    default Object getOriginalMqMessage() {
        return null;
    }

    default TypeReference<?> getTypeReference() {
        return null;
    }
}
