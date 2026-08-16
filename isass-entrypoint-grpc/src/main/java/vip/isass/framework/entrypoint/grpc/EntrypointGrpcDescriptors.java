// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.grpc;

import io.grpc.MethodDescriptor;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;

final class EntrypointGrpcDescriptors {

    private EntrypointGrpcDescriptors() {
    }

    static String serviceName(ServiceDefinition service) {
        return service.serviceName().replace('-', '_') + "."
                + service.contextName() + "." + service.resourceName();
    }

    static MethodDescriptor<byte[], byte[]> method(ServiceDefinition service, OperationDefinition operation) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(
                        serviceName(service), operation.operationName()))
                .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                .build();
    }
}
