// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request;

import vip.isass.framework.net.netty.request.worker.WorkerPool;

/**
 * 请求管理器
 * 根据客户端的消息命令，分发到各个指定的方法中去处理请求
 *
 * @author Rain
 */
public class RequestManager {

    /**
     * 处理客户端请求的线程池
     */
    private final WorkerPool workerPool;

    public RequestManager(WorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public void addRequest(Request request) {
        this.workerPool.putRequestInQueue(request);
    }

}
