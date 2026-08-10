// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.handler;

import vip.isass.framework.net.netty.request.Request;

/**
 * @author Rain
 */
public interface RequestHandler {

    /**
     * 执行处理
     *
     * @param request request
     */
    void handle(Request request);
}
