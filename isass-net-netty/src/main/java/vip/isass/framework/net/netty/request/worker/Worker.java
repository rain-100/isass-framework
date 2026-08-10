// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.worker;

/**
 * @author Rain
 */

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import vip.isass.framework.net.netty.request.worker.event.WorkCompletedEvent;
import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.worker.event.WorkExceptionEvent;
import vip.isass.framework.net.netty.request.worker.event.WorkStartEvent;

import jakarta.annotation.Resource;

@Slf4j
public abstract class Worker extends Thread {

    /**
     * 不精准的计数器
     */
    private volatile int workCount = 0;

    private boolean isStarted = false;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    protected void controlFlow() {
        while (isStarted) {
            Request request = pick();
            doWorkWrapper(request);
        }
    }

    /**
     * 选择一个网络请求
     *
     * @return requrst
     * @throws UnsupportedOperationException unsupported operation
     */
    protected Request pick() {
        throw new UnsupportedOperationException("请实现此方法");
    }

    /**
     * 执行业务流程
     *
     * @param request request
     * @throws Exception exception
     */
    protected abstract void doWork(Request request) throws Exception;

    protected void doWorkWrapper(Request request) {
        try {
            applicationEventPublisher.publishEvent(new WorkStartEvent().setRequest(request).setWorkCount(++workCount));
            doWork(request);
        } catch (Exception e) {
            applicationEventPublisher.publishEvent(new WorkExceptionEvent().setRequest(request).setException(e));
        } finally {
            applicationEventPublisher.publishEvent(new WorkCompletedEvent().setRequest(request));
        }
    }


    @Override
    public void run() {
        isStarted = true;
        controlFlow();
    }

}
