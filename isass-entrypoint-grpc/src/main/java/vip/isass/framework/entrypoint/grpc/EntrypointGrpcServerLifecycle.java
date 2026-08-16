// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;

final class EntrypointGrpcServerLifecycle implements SmartLifecycle {

    private final EntrypointGrpcProperties properties;
    private final EntrypointGrpcServerAdapter adapter;
    private volatile Server server;

    EntrypointGrpcServerLifecycle(EntrypointGrpcProperties properties,
                                  EntrypointGrpcServerAdapter adapter) {
        this.properties = properties;
        this.adapter = adapter;
    }

    @Override
    public void start() {
        if (properties.getServerPort() <= 0 || server != null) return;
        NettyServerBuilder builder = NettyServerBuilder.forPort(properties.getServerPort());
        adapter.serviceDefinitions().forEach(builder::addService);
        try {
            server = builder.build().start();
        } catch (IOException exception) {
            throw new IllegalStateException("启动 Entrypoint gRPC 服务失败", exception);
        }
    }

    @Override
    public void stop() {
        Server current = server;
        server = null;
        if (current != null) current.shutdown();
    }

    @Override public boolean isRunning() { return server != null && !server.isShutdown(); }
    @Override public boolean isAutoStartup() { return true; }
}
