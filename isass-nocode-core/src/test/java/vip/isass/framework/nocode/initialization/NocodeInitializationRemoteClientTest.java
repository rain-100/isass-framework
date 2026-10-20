// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.Sample;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.SampleCriteria;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.SampleService;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NocodeInitializationRemoteClientTest {

    @Test
    @SuppressWarnings("unchecked")
    void remoteExportPreservesHeadersAndExecutesTheAddressedOwnersLocalReadOnlyImplementation() throws Exception {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        var sample = new Sample();
        sample.setId(2081264465596485633L);
        sample.setName("远程,数据");
        when(repository.findAll()).thenReturn(List.of(sample));
        var local = new NocodeInitializationDataService(List.of(new SampleService(repository)));
        var registry = mock(ServiceDefinitionRegistry.class);
        when(registry.all()).thenReturn(List.of());
        var nextRemote = mock(INocodeInitializationService.class);
        var target = new NocodeInitializationController(local, new NocodeInitializationImporter(local, nextRemote, registry));
        var calls = new AtomicInteger();
        var observed = new AtomicReference<String>();
        var responseBody = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sample-service/nocode/system/initialization/exportData", exchange -> {
            calls.incrementAndGet();
            observed.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawQuery()
                    + " " + exchange.getRequestHeaders().getFirst("X-Export-Test"));
            byte[] response = responseBody.get() == null
                    ? JsonUtil.writeValueAsBytes(target.exportData("sample-service", List.of("sample"), "json").getBody())
                    : responseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            var provider = mock(AdditionalRequestHeaderProvider.class);
            when(provider.getHeaders(any())).thenReturn(Map.of("X-Export-Test", "preserved"));
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var remote = new NocodeInitializationRemoteClient(ignored -> endpoint, List.of(provider));
            var document = remote.exportData("sample-service", Set.of("sample"));
            assertThat(JsonUtil.writeValue(document)).contains("2081264465596485633", "远程,数据");
            assertThat(observed.get()).isEqualTo("GET entities=sample preserved");
            assertThat(calls.get()).isEqualTo(1);
            verify(repository).findAll();
            verifyNoInteractions(nextRemote);
            responseBody.set("{\"success\":false,\"message\":\"无导出权限\"}");
            assertThatThrownBy(() -> remote.exportData("sample-service", Set.of("sample")))
                    .hasMessageContaining("无导出权限");
            responseBody.set("{\"success\":true,\"data\":null}");
            assertThatThrownBy(() -> remote.exportData("sample-service", Set.of("sample")))
                    .hasMessageContaining("远程数据导出失败");
        } finally {
            server.stop(0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void remoteContractUsesTheImportDataEndpointAndExecutesLocallyWithoutForwarding() throws Exception {
        IRepository<Sample, SampleCriteria> repository = mock(IRepository.class);
        when(repository.findByCriteria(any())).thenReturn(List.of());
        var targetLocal = new NocodeInitializationDataService(List.of(new SampleService(repository)));
        var targetRemote = mock(INocodeInitializationService.class);
        var targetRegistry = mock(ServiceDefinitionRegistry.class);
        when(targetRegistry.all()).thenReturn(List.of());
        var target = new NocodeInitializationController(targetLocal,
                new NocodeInitializationImporter(targetLocal, targetRemote, targetRegistry));
        var count = new AtomicInteger();
        var header = new AtomicReference<String>();
        var body = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sample-service/nocode/system/initialization/importData", exchange -> {
            count.incrementAndGet();
            header.set(exchange.getRequestHeaders().getFirst("X-Initialization-Test"));
            byte[] request = exchange.getRequestBody().readAllBytes();
            body.set(new String(request, StandardCharsets.UTF_8));
            byte[] response = JsonUtil.writeValueAsBytes(target.importData("sample-service",
                    targetLocal.readDocument(new ByteArrayInputStream(request))));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            var provider = mock(AdditionalRequestHeaderProvider.class);
            when(provider.getHeaders(any())).thenReturn(Map.of("X-Initialization-Test", "preserved"));
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var remote = new NocodeInitializationRemoteClient(ignored -> endpoint, List.of(provider));
            var sourceRegistry = mock(ServiceDefinitionRegistry.class);
            var definition = NocodeInitializationImporterTest.definition("sample-service", "sample");
            when(sourceRegistry.all()).thenReturn(List.of(definition));
            var source = new NocodeInitializationImporter(new NocodeInitializationDataService(List.of()),
                    remote, sourceRegistry);
            Map<String, List<Map<String, Object>>> document = Map.of("sample",
                    List.of(Map.of("id", "2081264465596485633", "name", "remote")));

            var result = source.importData("entry-service", document);

            assertThat(result.inserted()).isEqualTo(1);
            assertThat(result.failures()).isEmpty();
            assertThat(count.get()).isEqualTo(1);
            assertThat(header.get()).isEqualTo("preserved");
            assertThat(body.get()).contains("2081264465596485633");
            verify(repository).add(any(Sample.class));
            verify(provider, times(1)).getHeaders(any());
            verifyNoInteractions(targetRemote);
        } finally {
            server.stop(0);
        }
    }
}
