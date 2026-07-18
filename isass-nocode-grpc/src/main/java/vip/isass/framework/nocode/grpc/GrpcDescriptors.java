package vip.isass.framework.nocode.grpc;

import io.grpc.MethodDescriptor;
import vip.isass.framework.nocode.contract.OperationContract;
import vip.isass.framework.nocode.contract.ServiceContract;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

final class GrpcDescriptors {

    private GrpcDescriptors() {
    }

    static MethodDescriptor<byte[], byte[]> method(
            ServiceContract service,
            OperationContract operation
    ) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(
                        service(service), upperFirst(operation.name())))
                .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                .build();
    }

    static MethodDescriptor<byte[], byte[]> fileStreamMethod(
            ServiceContract service,
            OperationContract operation
    ) {
        return MethodDescriptor.<byte[], byte[]>newBuilder()
                .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
                .setFullMethodName(MethodDescriptor.generateFullMethodName(
                        service(service), upperFirst(operation.name())))
                .setRequestMarshaller(ByteArrayMarshaller.INSTANCE)
                .setResponseMarshaller(ByteArrayMarshaller.INSTANCE)
                .build();
    }

    static String service(ServiceContract contract) {
        String interfaceName = contract.serviceInterface();
        int classSeparator = interfaceName.lastIndexOf('.');
        String packageName = interfaceName.substring(0, classSeparator)
                .replaceFirst("\\.api(?:\\..*)?$", ".nocode");
        String simpleName = interfaceName.substring(classSeparator + 1)
                .replaceFirst("^I", "");
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
                throw new IllegalStateException("Cannot read  gRPC payload", exception);
            }
        }
    }
}
