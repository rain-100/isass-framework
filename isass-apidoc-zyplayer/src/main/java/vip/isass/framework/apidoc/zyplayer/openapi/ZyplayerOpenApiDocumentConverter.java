package vip.isass.framework.apidoc.zyplayer.openapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.util.StringUtils;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerEditorTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Rain
 */
public class ZyplayerOpenApiDocumentConverter {

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "delete", "patch", "head", "options");

    private final ObjectMapper objectMapper;

    private final ZyplayerOpenApiExcludeRules excludeRules;

    public ZyplayerOpenApiDocumentConverter(ObjectMapper objectMapper) {
        this(objectMapper, new ZyplayerOpenApiExcludeRules(List.of(), List.of(), List.of()));
    }

    public ZyplayerOpenApiDocumentConverter(ObjectMapper objectMapper, ZyplayerOpenApiExcludeRules excludeRules) {
        this.objectMapper = objectMapper;
        this.excludeRules = excludeRules;
    }

    public List<ZyplayerSyncDocument> convert(String openApiJson, String apiBaseUrl) {
        try {
            JsonNode openApi = objectMapper.readTree(openApiJson);
            List<ZyplayerSyncDocument> documents = new ArrayList<>();
            JsonNode paths = openApi.path("paths");
            if (!paths.isObject()) {
                return documents;
            }
            for (Iterator<Map.Entry<String, JsonNode>> pathIterator = paths.properties().iterator(); pathIterator.hasNext(); ) {
                Map.Entry<String, JsonNode> pathEntry = pathIterator.next();
                String path = pathEntry.getKey();
                JsonNode pathItem = pathEntry.getValue();
                for (String method : HTTP_METHODS) {
                    JsonNode operation = pathItem.path(method);
                    if (!operation.isObject()) {
                        continue;
                    }
                    if (excludeRules.matches(method, path, controllerNames(operation))) {
                        continue;
                    }
                    documents.add(toDocument(openApi, path, method, operation, apiBaseUrl));
                }
            }
            return documents;
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to convert openapi to zyplayer api documents", e);
        }
    }

    private List<String> controllerNames(JsonNode operation) {
        List<String> names = new ArrayList<>();
        addText(names, operation.path("x-controller"));
        addText(names, operation.path("x-className"));
        addText(names, operation.path("x-class-name"));
        addText(names, operation.path("x-source-class"));
        JsonNode extensions = operation.path("extensions");
        if (extensions.isObject()) {
            addText(names, extensions.path("x-controller"));
            addText(names, extensions.path("x-className"));
        }
        return names;
    }

    private void addText(List<String> values, JsonNode node) {
        if (node.isTextual() && StringUtils.hasText(node.asText())) {
            values.add(node.asText());
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                addText(values, item);
            }
        }
    }

    private ZyplayerSyncDocument toDocument(JsonNode openApi, String path, String method, JsonNode operation, String apiBaseUrl) throws Exception {
        ObjectNode content = objectMapper.createObjectNode();
        content.put("memos", "");
        content.put("method", method);
        content.put("apiUrl", joinUrl(apiBaseUrl, path));
        content.put("description", text(operation, "description", text(operation, "summary", "")));
        content.set("urlParams", objectMapper.createArrayNode());
        content.set("pathParams", objectMapper.createArrayNode());
        content.set("formParams", objectMapper.createArrayNode());
        content.set("headerParams", objectMapper.createArrayNode());
        content.set("cookieParams", objectMapper.createArrayNode());
        content.set("formEncodeParams", objectMapper.createArrayNode());
        content.set("bodyRowParams", objectMapper.createArrayNode());
        content.set("beforeActions", actionArray());
        content.set("afterActions", actionArray());
        content.put("bodyParamType", "none");
        content.put("bodyConsumesType", "json");
        content.put("bodyParamRow", "");
        content.set("bodyParamObj", schemaNode(objectMapper.createObjectNode().put("type", "object")));
        content.set("response", responseArray(openApi, operation.path("responses")));

        applyParameters(content, operation.path("parameters"));
        applyRequestBody(openApi, content, operation.path("requestBody"));

        String title = operationTitle(path, method, operation);
        return new ZyplayerSyncDocument("api/" + method + "/" + normalizePath(path), title, ZyplayerEditorTypes.API,
                objectMapper.writeValueAsString(content), List.of("api接口", groupName(operation)));
    }

    private String operationTitle(String path, String method, JsonNode operation) {
        String summary = text(operation, "summary", "");
        if (StringUtils.hasText(summary)) {
            return summary;
        }
        String operationId = text(operation, "operationId", "");
        if (StringUtils.hasText(operationId)) {
            return operationId;
        }
        return method.toUpperCase(Locale.ROOT) + " " + path;
    }

    private String groupName(JsonNode operation) {
        JsonNode tags = operation.path("tags");
        if (tags.isArray() && !tags.isEmpty()) {
            String tag = tags.get(0).asText("");
            if (StringUtils.hasText(tag)) {
                return tag;
            }
        }
        return "默认接口";
    }

    private void applyParameters(ObjectNode content, JsonNode parameters) {
        if (!parameters.isArray()) {
            return;
        }
        for (JsonNode parameter : parameters) {
            String in = text(parameter, "in", "");
            ArrayNode target = switch (in) {
                case "query" -> (ArrayNode) content.path("urlParams");
                case "path" -> (ArrayNode) content.path("pathParams");
                case "header" -> (ArrayNode) content.path("headerParams");
                case "cookie" -> (ArrayNode) content.path("cookieParams");
                default -> null;
            };
            if (target != null) {
                target.add(paramNode(parameter));
            }
        }
    }

    private void applyRequestBody(JsonNode openApi, ObjectNode content, JsonNode requestBody) {
        JsonNode requestContent = requestBody.path("content");
        if (!requestContent.isObject()) {
            return;
        }
        if (requestContent.has("multipart/form-data")) {
            content.put("bodyParamType", "form");
            addFormSchema(openApi, (ArrayNode) content.path("formParams"),
                    requestContent.path("multipart/form-data").path("schema"));
            return;
        }
        if (requestContent.has("application/x-www-form-urlencoded")) {
            content.put("bodyParamType", "formEncode");
            addFormSchema(openApi, (ArrayNode) content.path("formEncodeParams"),
                    requestContent.path("application/x-www-form-urlencoded").path("schema"));
            return;
        }
        JsonNode media = requestContent.has("application/json")
                ? requestContent.path("application/json")
                : firstPropertyValue(requestContent);
        JsonNode schema = resolve(openApi, media.path("schema"));
        if (schema.isObject()) {
            content.put("bodyParamType", "row");
            content.put("bodyConsumesType", requestContent.has("application/xml") ? "xml" : "json");
            ObjectNode bodyParamObj = schemaNode(schema);
            content.set("bodyParamObj", bodyParamObj);
            content.put("bodyParamRow", example(bodyParamObj));
        }
    }

    private void addFormSchema(JsonNode openApi, ArrayNode target, JsonNode schema) {
        JsonNode resolved = resolve(openApi, schema);
        JsonNode properties = resolved.path("properties");
        if (!properties.isObject()) {
            return;
        }
        for (Iterator<Map.Entry<String, JsonNode>> iterator = properties.properties().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            ObjectNode parameter = objectMapper.createObjectNode();
            parameter.put("name", entry.getKey());
            parameter.put("type", schemaType(resolve(openApi, entry.getValue())));
            parameter.put("desc", text(entry.getValue(), "description", ""));
            parameter.put("key", key(entry.getKey()));
            target.add(parameter);
        }
    }

    private ArrayNode responseArray(JsonNode openApi, JsonNode responses) {
        ArrayNode array = objectMapper.createArrayNode();
        if (!responses.isObject()) {
            array.add(defaultResponse(200, "OK", objectMapper.createObjectNode().put("type", "object")));
            return array;
        }
        for (Iterator<Map.Entry<String, JsonNode>> iterator = responses.properties().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            int status = parseStatus(entry.getKey());
            JsonNode response = entry.getValue();
            JsonNode content = response.path("content");
            String contentType = content.has("application/json") ? "json" : "json";
            JsonNode media = content.has("application/json")
                    ? content.path("application/json")
                    : firstPropertyValue(content);
            array.add(defaultResponse(status, text(response, "description", entry.getKey()), resolve(openApi, media.path("schema"))));
        }
        return array;
    }

    private ObjectNode defaultResponse(int statusCode, String name, JsonNode schema) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("name", StringUtils.hasText(name) ? name : "成功");
        response.put("statusCode", statusCode);
        response.put("contentType", "json");
        response.put("locked", false);
        response.put("key", key("response-" + statusCode));
        response.put("dataContent", "");
        response.set("data", schemaNode(schema.isObject() ? schema : objectMapper.createObjectNode().put("type", "object")));
        return response;
    }

    private ObjectNode paramNode(JsonNode parameter) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("name", text(parameter, "name", ""));
        node.put("type", schemaType(parameter.path("schema")));
        node.put("desc", text(parameter, "description", ""));
        node.put("required", parameter.path("required").asBoolean(false));
        node.put("key", key(text(parameter, "name", "param")));
        return node;
    }

    private ObjectNode schemaNode(JsonNode schema) {
        JsonNode resolved = schema;
        ObjectNode node = objectMapper.createObjectNode();
        String type = schemaType(resolved);
        node.put("type", type);
        node.put("name", text(resolved, "name", ""));
        node.put("desc", text(resolved, "description", ""));
        node.put("readonly", false);
        node.put("example", exampleValue(type));
        ArrayNode children = objectMapper.createArrayNode();
        JsonNode childProperties = "array".equals(type) ? resolved.path("items").path("properties") : resolved.path("properties");
        if (childProperties.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> iterator = childProperties.properties().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                ObjectNode child = schemaNode(entry.getValue());
                child.put("name", entry.getKey());
                children.add(child);
            }
        }
        node.set("children", children);
        return node;
    }

    private JsonNode resolve(JsonNode openApi, JsonNode schema) {
        String ref = schema.path("$ref").asText("");
        if (!ref.startsWith("#/")) {
            return schema;
        }
        JsonNode node = openApi;
        for (String segment : ref.substring(2).split("/")) {
            node = node.path(segment);
        }
        return node.isMissingNode() ? schema : node;
    }

    private String example(ObjectNode schema) {
        try {
            return objectMapper.writeValueAsString(exampleJson(schema));
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode exampleJson(JsonNode schema) {
        String type = schema.path("type").asText("object");
        if ("object".equals(type)) {
            ObjectNode object = objectMapper.createObjectNode();
            for (JsonNode child : schema.path("children")) {
                String name = child.path("name").asText("");
                if (StringUtils.hasText(name)) {
                    object.set(name, exampleJson(child));
                }
            }
            return object;
        }
        if ("array".equals(type)) {
            ArrayNode array = objectMapper.createArrayNode();
            if (schema.path("children").isArray() && !schema.path("children").isEmpty()) {
                array.add(exampleJson(schema.path("children").get(0)));
            }
            return array;
        }
        if ("boolean".equals(type)) {
            return objectMapper.getNodeFactory().booleanNode(false);
        }
        if ("number".equals(type) || "integer".equals(type)) {
            return objectMapper.getNodeFactory().numberNode(0);
        }
        return objectMapper.getNodeFactory().textNode("");
    }

    private String schemaType(JsonNode schema) {
        String type = schema.path("type").asText("object");
        if ("string".equals(type) && "binary".equals(schema.path("format").asText())) {
            return "file";
        }
        if ("integer".equals(type)) {
            return "integer";
        }
        return switch (type) {
            case "array", "boolean", "number", "object", "string", "file" -> type;
            default -> "object";
        };
    }

    private String text(JsonNode node, String field, String defaultValue) {
        String value = node.path(field).asText("");
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private JsonNode firstPropertyValue(JsonNode node) {
        Iterator<Map.Entry<String, JsonNode>> iterator = node.properties().iterator();
        return iterator.hasNext() ? iterator.next().getValue() : objectMapper.createObjectNode();
    }

    private int parseStatus(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 200;
        }
    }

    private String joinUrl(String baseUrl, String path) {
        String base = StringUtils.hasText(baseUrl) ? baseUrl : "";
        if (!StringUtils.hasText(base)) {
            return path;
        }
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private String normalizePath(String path) {
        return path.replaceAll("^/+", "").replaceAll("/+", "/");
    }

    private ArrayNode actionArray() {
        ArrayNode array = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("content", "");
        action.put("type", "js");
        array.add(action);
        return array;
    }

    private String key(String seed) {
        String normalized = seed == null ? "item" : seed.replaceAll("[^A-Za-z0-9]", "");
        return "isass" + (normalized.isBlank() ? "item" : normalized);
    }

    private String exampleValue(String type) {
        return switch (type) {
            case "boolean" -> "false";
            case "number", "integer" -> "0";
            default -> "";
        };
    }
}
