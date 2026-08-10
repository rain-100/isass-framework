// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.request.worker.sequential;

import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.handler.RequestHandler;
import vip.isass.framework.net.netty.request.worker.Worker;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author hone 2018/5/7
 * // todo 从语义上，或代码可读性角度来说，一个Worker应该为一个线程，而不是一个runnable。具体处理一个task时，这个task，才是一个runnable
 */
@Slf4j
public class SequentialWorker extends Worker {

    private SequentialWorkerPool sequentialWorkerPool;

    private RequestHandler requestHandler;

    /**
     * 临时线程
     */
    private final boolean isCore;

    public SequentialWorker(SequentialWorkerPool sequentialWorkerPool) {
        this(sequentialWorkerPool, true);
    }

    public SequentialWorker(SequentialWorkerPool sequentialWorkerPool, boolean isCore) {
        this.sequentialWorkerPool = sequentialWorkerPool;
        this.isCore = isCore;
    }

    @Override
    protected void controlFlow() {
        // 临时线程是可能回收的
        while (true) {
            sequentialWorkerPool
                    .getWorkMap()
                    .forEach((k, v) -> {
                        if (!v.isEmpty()) {
                            ConcurrentLinkedQueue<Request> requests = sequentialWorkerPool
                                    .getWorkingMap()
                                    .putIfAbsent(k, v);

                            if (requests == null) {
                                while (true) {
                                    Request request = v.poll();
                                    if (request == null) {
                                        break;
                                    }
                                    doWorkWrapper(request);
                                }
                                // todo 有并发问题。remove时，可能有新任务被添加了，此时remove，就把任务抛弃了。另外workMap也要remove

                                // todo 当线程为5时，有5个group并行处理中，并且这5个group不断请求进来，
                                // todo 那么第6个group就只能等到前5个group的其中一个不请求了，才会轮到第6个处理业务。
                                // todo 即新group的优先级永远低于已经在处理中的group
                                sequentialWorkerPool.getWorkingMap().remove(k);
                            }
                        }
                    });

            if (!isCore){
                System.out.println(Thread.currentThread().getName());
                return;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private static Set<String> set = new HashSet<>();
    @Override
    protected void doWork(Request request) {
        String name = Thread.currentThread().getName();
        if (set.add(name)){
            System.out.println(name);
        }
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // todo 调试
        // requestHandler.handle(request);

    }

}
