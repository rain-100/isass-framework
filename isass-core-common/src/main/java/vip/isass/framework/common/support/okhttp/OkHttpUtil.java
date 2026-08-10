// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.okhttp;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import tools.jackson.core.type.TypeReference;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import vip.isass.framework.common.support.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpUtil {
    public static final OkHttpClient CLIENT;

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(100);
        dispatcher.setMaxRequestsPerHost(100);
        CLIENT = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .pingInterval(7000, TimeUnit.MILLISECONDS)
                .dispatcher(dispatcher)
                .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES))
                .build();
    }

    public static HttpUrl newHttpUrl(String urlTemplate, String[] pathVariables, Map<String, ?> queryParams) {
        HttpUrl httpUrl = HttpUrl.parse(urlTemplate);
        HttpUrl.Builder builder = httpUrl.newBuilder();
        if (ArrayUtil.isNotEmpty(pathVariables)) {
            int idx = 0;
            for (int i = 0; i < httpUrl.pathSegments().size(); i++) {
                String pathSegment = httpUrl.pathSegments().get(i);
                if (pathSegment.startsWith("{") && pathSegment.endsWith("}")) {
                    builder.setPathSegment(i, pathVariables[idx++]);
                }
            }
        }
        if (MapUtil.isNotEmpty(queryParams)) {
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                String valStr;
                Object value = entry.getValue();
                if (value instanceof Iterable) {
                    valStr = CollUtil.join((Iterable<?>) value, ",");
                } else {
                    valStr = value.toString();
                }
                builder.addQueryParameter(entry.getKey(), valStr);
            }
        }
        return builder.build();
    }

    @SneakyThrows
    public static Response get(String url) {
        Request request = new Request.Builder().url(url).get().build();
        Response response = CLIENT.newCall(request).execute();
        return response;
    }

    public static <T> T get(String url, Class<T> clazz) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            String bodyStr = body == null ? "" : body.string();
            if (execute.isSuccessful()) {
                return JsonUtil.readValue(bodyStr, clazz);
            }
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static <T> T get(String url, TypeReference<T> typeReference) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            String bodyStr = body == null ? "" : body.string();
            if (execute.isSuccessful()) {
                return JsonUtil.readValue(bodyStr, typeReference);
            } else if (execute.code() == 404 && JSONUtil.isJson(bodyStr)) {
                return JsonUtil.readValue(bodyStr, typeReference);
            }
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static <T> T get(String urlTemplate,
                            String[] pathVariables,
                            Map<String, ?> queryParams,
                            TypeReference<T> typeReference) {
        HttpUrl httpUrl = newHttpUrl(urlTemplate, pathVariables, queryParams);
        Request request = new Request.Builder().url(httpUrl).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            String bodyStr = body == null ? "" : body.string();
            if (execute.isSuccessful()) {
                return JsonUtil.readValue(bodyStr, typeReference);
            } else if (execute.code() == 404 && JSONUtil.isJson(bodyStr)) {
                return JsonUtil.readValue(bodyStr, typeReference);
            }
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static String getAsString(String url) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            String bodyStr = body == null ? "" : body.string();
            if (execute.isSuccessful()) {
                return bodyStr;
            } else if (execute.code() == 404 && JSONUtil.isJson(bodyStr)) {
                return bodyStr;
            }
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static byte[] getAsBytes(String url) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            if (execute.isSuccessful()) {
                return body == null ? null : body.bytes();
            }
            String bodyStr = body == null ? "" : body.string();
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static InputStream getAsInputStream(String url) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            if (execute.isSuccessful()) {
                return body == null ? null : body.byteStream();
            }
            String bodyStr = body == null ? "" : body.string();
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static Map<String, Object> getAsMap(String url) {
        Request request = new Request.Builder().url(url).get().build();
        Call call = CLIENT.newCall(request);
        try (Response execute = call.execute();) {
            ResponseBody body = execute.body();
            String bodyStr = body == null ? "" : body.string();
            if (execute.isSuccessful()) {
                return JsonUtil.readMap(bodyStr);
            }
            throw new RuntimeException("调用" + request + " 失败，状态码：" + execute.code() + " 响应体：" + bodyStr);
        } catch (IOException e) {
            throw new RuntimeException("远程调用失败：" + request, e);
        }
    }

    @SneakyThrows
    public static Response post(String url, String[] pathVariables, Map<String, Object> queryParams, Object body) {
        HttpUrl httpUrl = newHttpUrl(url, pathVariables, queryParams);
        RequestBody requestBody = RequestBody.create(
                okhttp3.MediaType.get("application/json"),
                JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(body));
        Request request = new Request.Builder().post(requestBody).url(httpUrl).build();
        return CLIENT.newCall(request).execute();
    }

    @SneakyThrows
    public static <T> T post(String url, String[] pathVariables, Map<String, Object> queryParams, Object body, Class<T> clazz) {
        return JsonUtil.readValue(post(url, pathVariables, queryParams, body).body().string(), clazz);
    }

    @SneakyThrows
    public static <T> T post(String url, String[] pathVariables, Map<String, Object> queryParams, Object body, TypeReference<T> typeReference) {
        return JsonUtil.readValue(post(url, pathVariables, queryParams, body).body().string(), typeReference);
    }

    @SneakyThrows
    public static InputStream postJsonAsInputStream(String url, String[] pathVariables, Map<String, Object> queryParams, Object body) {
        return post(url, pathVariables, queryParams, body).body().byteStream();
    }

    public static WebSocket newWebsocket(String wsUrl,
                                         OnSocketConnectedHandler onSocketConnectedHandler,
                                         OnSocketMessageHandler onSocketMessageHandler,
                                         OnSocketClosingHandler onSocketClosingHandler) {
        Request request = new Request.Builder().url(wsUrl).build();
        return CLIENT.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(final WebSocket webSocket, Response response) {
                log.debug("onOpen {}, {}", webSocket, response);
                if (onSocketConnectedHandler != null) {
                    onSocketConnectedHandler.handle(webSocket);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String message) {
                log.debug("client onMessage: {}", message);
                if (onSocketMessageHandler != null) {
                    onSocketMessageHandler.handle(webSocket, message);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.debug("onClosing {}, {}, {}", webSocket, code, reason);
                if (onSocketClosingHandler != null) {
                    onSocketClosingHandler.handle(webSocket, code, reason);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.debug("onFailure {}, {}, {}", webSocket, t, response);
                webSocket.close(1000, "");
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.debug("onClosed {}, {}, {}", webSocket, code, reason);
            }
        });
    }

}
