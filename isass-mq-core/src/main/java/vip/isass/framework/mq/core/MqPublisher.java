// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core;

import lombok.NonNull;

public final class MqPublisher {

    private static MqManager mqManager;

    private MqPublisher() {
    }

    static void setMqManager(MqManager mqManager) {
        MqPublisher.mqManager = mqManager;
    }

    public static void send(@NonNull MqMessage mqMessage) {
        requireMqManager().send(mqMessage);
    }

    public static void send(@NonNull String sourceName, @NonNull MqMessage mqMessage) {
        requireMqManager().send(sourceName, mqMessage);
    }

    private static MqManager requireMqManager() {
        if (mqManager == null) {
            throw new IllegalStateException("mq manager is not initialized");
        }
        return mqManager;
    }
}
