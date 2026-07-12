package vip.isass.framework.nocode.grpc;

import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;
import vip.isass.framework.nocode.stream.FileStream;

@FunctionalInterface
public interface GrpcInvocationHandler {

    byte[] invoke(
            ServiceContract service,
            OperationContract operation,
            byte[] request
    );

    /**
     * 调用返回文件流的  方法。默认实现使已有 unary handler 保持二进制兼容。
     */
    default FileStream invokeFile(
            ServiceContract service,
            OperationContract operation,
            byte[] request
    ) {
        throw new UnsupportedOperationException(" gRPC file streaming is not supported: " + operation.name());
    }
}
