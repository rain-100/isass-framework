package vip.isass.framework.web.response;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.bind.annotation.ControllerAdvice;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.nocode.entity.AdvancedFeature;
import vip.isass.framework.nocode.entity.IAnyJsonEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Applies explicit HTTP response-projection options without ThreadLocal state. */
@ControllerAdvice
public class AdvancedFeatureResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public AdvancedFeatureResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse ignoredResponse) {
        if (body == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return body;
        }
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return body;
        }
        AdvancedFeature feature = AdvancedFeatureResolver.resolve(servletRequest.getServletRequest());
        if (feature == null) {
            return body;
        }
        if (body instanceof Resp resp) {
            resp.setData(project(resp.getData(), feature));
            return resp;
        }
        return project(body, feature);
    }

    private Object project(Object value, AdvancedFeature feature) {
        if (value == null) {
            return null;
        }
        if (value instanceof IAnyJsonEntity entity) {
            Map<String, Object> result = objectMapper.convertValue(entity, Map.class);
            Map<String, Object> extra = entity.advancedJson(feature);
            if (extra != null) {
                result.putAll(extra);
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(item -> project(item, feature)).toList();
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(key, project(item, feature)));
            return result;
        }
        // MyBatis-Plus pages expose getRecords/setRecords; use reflection to avoid a database dependency here.
        try {
            Method getRecords = value.getClass().getMethod("getRecords");
            Object records = getRecords.invoke(value);
            if (records instanceof Collection<?> collection) {
                Method setRecords = value.getClass().getMethod("setRecords", List.class);
                setRecords.invoke(value, new ArrayList<>(collection.stream().map(item -> project(item, feature)).toList()));
            }
        } catch (ReflectiveOperationException ignored) {
            // Not a page type.
        }
        return value;
    }
}
