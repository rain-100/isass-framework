// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.mq.spring.event;

import org.springframework.context.ApplicationEvent;
import vip.isass.framework.mq.core.MqMessageContext;

public class IsassMqEvent extends ApplicationEvent {

    public IsassMqEvent(MqMessageContext source) {
        super(source);
    }

}
