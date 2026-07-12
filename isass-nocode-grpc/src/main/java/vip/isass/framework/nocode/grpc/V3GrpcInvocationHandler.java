package vip.isass.framework.nocode.grpc;

import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.stream.V3FileStream;

@FunctionalInterface
public interface V3GrpcInvocationHandler {

    byte[] invoke(
            V3ServiceContract service,
            V3OperationContract operation,
            byte[] request
    );

    /**
     * 调用返回文件流的 V3 方法。默认实现使已有 unary handler 保持二进制兼容。
     */
    default V3FileStream invokeFile(
            V3ServiceContract service,
            V3OperationContract operation,
            byte[] request
    ) {
        throw new UnsupportedOperationException("V3 gRPC file streaming is not supported: " + operation.name());
    }
}
