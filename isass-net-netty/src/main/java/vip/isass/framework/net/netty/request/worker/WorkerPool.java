// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.worker;

import vip.isass.framework.net.netty.request.Request;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author hone 2018/5/8
 */
public interface WorkerPool extends InitializingBean {

    /**
     * 将一个网络请求，放到请求处理器的队列中，等待空闲业务线程处理
     *
     * @param request request
     */
    void putRequestInQueue(Request request);

    /**
     * 初始化
     * 创建此 bean 时，会自动被 spring 调用一次
     *
     * @return worker pool
     */
    WorkerPool init();

    @Override
    default void afterPropertiesSet() {
        init();
    }
}
