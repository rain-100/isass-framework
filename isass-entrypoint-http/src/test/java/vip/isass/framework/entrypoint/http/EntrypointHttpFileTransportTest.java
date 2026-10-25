// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.entrypoint.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.registry.EntrypointDefinitionParser;
import vip.isass.framework.entrypoint.stream.FileStream;
import vip.isass.framework.entrypoint.transport.EntrypointRemoteBusinessException;
import vip.isass.framework.entrypoint.transport.EntrypointTransportException;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EntrypointHttpFileTransportTest {
    @EntrypointInfo(serviceName = "sample-service", contextName = "file", resourceName = "image")
    interface ImageService extends IEntrypoint {
        @EntrypointOperation(operationName = "download", displayName = "下载", httpMethod = HttpMethod.GET)
        FileStream download();
    }

    @Test
    void binaryResponsePreservesMetadataAndOwnsConnectionUntilConsumed() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        byte[] content = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0, 1, 2};
        var closed = new AtomicInteger();
        server.expect(requestTo("http://localhost/sample-service/file/image/download"))
                .andExpect(header(HttpHeaders.ACCEPT, "*/*"))
                .andRespond(request -> {
                    var response = new MockClientHttpResponse(content, HttpStatus.OK) {
                        @Override
                        public void close() {
                            closed.incrementAndGet();
                            super.close();
                        }
                    };
                    response.getHeaders().setContentType(MediaType.IMAGE_PNG);
                    response.getHeaders().setContentLength(content.length);
                    response.getHeaders().setContentDisposition(ContentDisposition.inline().filename("参考图.png", StandardCharsets.UTF_8).build());
                    return response;
                });
        var service = new EntrypointDefinitionParser(List.of()).parse(ImageService.class, false);
        var transport = new EntrypointHttpTransport(builder.build(), ignored -> URI.create("http://localhost"), List.of());
        try (var stream = assertInstanceOf(FileStream.class, transport.invoke(service, service.operations().getFirst(), new Object[0]))) {
            assertEquals(0, closed.get());
            assertEquals("参考图.png", stream.fileName());
            assertEquals("image/png", stream.contentType());
            assertEquals((long) content.length, stream.contentLength());
            assertFalse(stream.download());
            var output = new ByteArrayOutputStream();
            stream.writeTo(output);
            assertArrayEquals(content, output.toByteArray());
        }
        assertEquals(1, closed.get());
        server.verify();
    }

    @Test
    void fileEndpointBusinessFailureIsNotReturnedAsAFileOrRetried() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/sample-service/file/image/download"))
                .andRespond(withSuccess("{\"success\":false,\"message\":\"没有附件权限\"}", MediaType.APPLICATION_JSON));
        var service = new EntrypointDefinitionParser(List.of()).parse(ImageService.class, false);
        var transport = new EntrypointHttpTransport(builder.build(), ignored -> URI.create("http://localhost"), List.of());
        assertEquals("没有附件权限", assertThrows(EntrypointRemoteBusinessException.class,
                () -> transport.invoke(service, service.operations().getFirst(), new Object[0])).getMessage());
        server.verify();
    }

    @Test
    void fileEndpointHttpErrorIsAnAlreadySentFailure() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/sample-service/file/image/download"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        var service = new EntrypointDefinitionParser(List.of()).parse(ImageService.class, false);
        var transport = new EntrypointHttpTransport(builder.build(), ignored -> URI.create("http://localhost"), List.of());
        var failure = assertThrows(EntrypointTransportException.class,
                () -> transport.invoke(service, service.operations().getFirst(), new Object[0]));
        assertFalse(failure.unavailableBeforeSend());
        server.verify();
    }
}
