// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.core.producer;

import vip.isass.framework.mq.core.MqMessageContext;

/**
 * @author Rain
 */
public interface MqProducer {

    MqProducer init();

    void destroy();

    void send(MqMessageContext mqMessageContext);

}
