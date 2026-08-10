// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.worker.event;

import vip.isass.framework.net.netty.request.Request;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

/**
 * @author hone
 * @since 2018/5/18
 */
@Getter
@Setter
@Accessors(chain = true)
public class WorkExceptionEvent extends ApplicationEvent {

    private Request request;

    private Exception exception;

    public WorkExceptionEvent() {
        super(Boolean.TRUE);
    }

}
