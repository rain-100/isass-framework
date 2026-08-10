// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.consumer;


/**
 * @author Rain
 */
public interface MqConsumerManager {

    String getManufacturer();

    void subscribe();

    void destroy();

    boolean isEnable();

}
