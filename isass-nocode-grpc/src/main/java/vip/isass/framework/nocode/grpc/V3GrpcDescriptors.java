package vip.isass.framework.nocode.grpc;

import io.grpc.MethodDescriptor;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class V3GrpcDescriptors {

    private V3GrpcDescriptors() {
    }

    static MethodDescriptor<byte[], byte[]> method(
            V3ServiceContract service,
            V3OperationContract operation
    ) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(
                        serviceName(service), upperFirst(operation.name())))
                .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                .build();
    }

    static String serviceName(V3ServiceContract contract) {
        String interfaceName = contract.serviceInterface();
        int classSeparator = interfaceName.lastIndexOf('.');
        String packageName = interfaceName.substring(0, classSeparator)
                .replaceFirst("\\.api(?:\\..*)?$", ".v3");
        String simpleName = interfaceName.substring(classSeparator + 1)
                .replaceFirst("^IV3", "");
        return packageName + "." + simpleName;
    }

    private static String upperFirst(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private enum ByteArrayMarshaller implements MethodDescriptor.Marshaller<byte[]> {
        INSTANCE;

        public InputStream stream(byte[] value) {
            return new ByteArrayInputStream(value);
        }

        public byte[] parse(InputStream stream) {
            try {
                return stream.readAllBytes();
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot read V3 gRPC payload", exception);
            }
        }
    }
}
