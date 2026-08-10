// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.worker.normal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Scope;
import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.handler.RequestHandler;
import vip.isass.framework.net.netty.request.worker.Worker;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * @author Rain
 */
@Slf4j
@ConditionalOnMissingBean(Worker.class)
@Scope("prototype")
public class DefaultWorker extends Worker {

    private final RequestHandler requestHandler;

    private static final int MAX_QUEUE_SIZE = Math.min(1000, 1000);

    private BlockingQueue<Request> blockingQueue;

    public DefaultWorker(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        this.blockingQueue = new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
        this.setDaemon(true);
    }

    @Override
    @SneakyThrows
    protected Request pick() {
        return this.blockingQueue.take();
    }

    @Override
    protected void doWork(Request request) {
        requestHandler.handle(request);
    }

    /**
     * 接收一个事件请求，放入队列中
     *
     * @param request request
     */
    public final void acceptRequest(Request request) {
        boolean ok = this.blockingQueue.offer(request);
        if (!ok) {
            log.error("添加请求到 请求队列 失败！");
        }
    }

}
