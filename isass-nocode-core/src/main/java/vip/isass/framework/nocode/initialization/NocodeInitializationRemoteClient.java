// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderContext;
import vip.isass.framework.common.web.header.AdditionalRequestHeaderProvider;
import vip.isass.framework.entrypoint.http.HttpEndpointResolver;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NocodeInitializationRemoteClient implements INocodeInitializationService {

    private final HttpEndpointResolver endpoints;
    private final List<AdditionalRequestHeaderProvider> headerProviders;

    public NocodeInitializationRemoteClient(HttpEndpointResolver endpoints,
                                            List<AdditionalRequestHeaderProvider> headerProviders) {
        this.endpoints = endpoints;
        this.headerProviders = List.copyOf(headerProviders);
    }

    @Override
    public ImportResult importData(
            String serviceName, Map<String, ? extends Collection<?>> document) {
        URI endpoint = endpoints.resolve(serviceName);
        if (endpoint == null) throw new IllegalStateException("未配置初始化目标地址: " + serviceName);
        URI uri = endpoint.resolve("/" + serviceName + "/nocode/system/initialization/importData");
        byte[] serializedBody = JsonUtil.writeValueAsBytes(document);
        HttpHeaders headers = new HttpHeaders();
        AdditionalRequestHeaderContext headerContext = new AdditionalRequestHeaderContext(
                "POST", uri, serializedBody);
        for (AdditionalRequestHeaderProvider provider : headerProviders) {
            provider.getHeaders(headerContext).forEach((name, value) -> {
                if (provider.override() || headers.getFirst(name) == null) headers.set(name, value);
            });
        }
        JsonNode response = RestClient.create().post().uri(uri)
                .headers(target -> target.addAll(headers))
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .body(serializedBody).retrieve().body(JsonNode.class);
        if (response == null || !response.path("success").asBoolean()) {
            String message = response == null ? "无响应" : response.path("detailMessage").asString();
            if (message.isBlank()) {
                message = response.path("message").asString();
            }
            throw new IllegalStateException("远程初始化失败 " + serviceName + ": " + message);
        }
        return JsonUtil.convertValue(response.path("data"),
                ImportResult.class);
    }

    @Override
    public Map<String, List<?>> exportData(String serviceName, Collection<String> entityNames) {
        URI endpoint = endpoints.resolve(serviceName);
        if (endpoint == null) throw new IllegalStateException("未配置导出目标地址: " + serviceName);
        URI uri = UriComponentsBuilder.fromUri(endpoint.resolve(
                        "/" + serviceName + "/nocode/system/initialization/exportData"))
                .queryParam("entities", String.join(",", entityNames)).build().encode().toUri();
        HttpHeaders headers = new HttpHeaders();
        var context = new AdditionalRequestHeaderContext("GET", uri, new byte[0]);
        for (AdditionalRequestHeaderProvider provider : headerProviders) {
            provider.getHeaders(context).forEach((name, value) -> {
                if (provider.override() || headers.getFirst(name) == null) headers.set(name, value);
            });
        }
        JsonNode response = RestClient.create().get().uri(uri).headers(target -> target.addAll(headers))
                .accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
        if (response == null || !response.path("success").asBoolean() || !response.path("data").isObject()) {
            String message = response == null ? "无响应" : response.path("detailMessage").asString();
            if (message.isBlank()) {
                message = response.path("message").asString();
            }
            throw new IllegalStateException("远程数据导出失败 " + serviceName + ": " + message);
        }
        return JsonUtil.readValue(response.path("data").toString(),
                new TypeReference<LinkedHashMap<String, List<?>>>() { });
    }
}
