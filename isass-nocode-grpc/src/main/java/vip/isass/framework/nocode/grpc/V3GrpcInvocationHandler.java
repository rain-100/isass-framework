package vip.isass.framework.nocode.grpc;

import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;

@FunctionalInterface
public interface V3GrpcInvocationHandler {

    byte[] invoke(
            V3ServiceContract service,
            V3OperationContract operation,
            byte[] request
    );
}
