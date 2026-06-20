package vip.isass.framework.apidoc.zyplayer.client;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rain
 */
public class ZyplayerOpenApiClient implements ZyplayerClientOperations {

    private static final int SUCCESS_CODE = 200;

    private static final List<String> COLLECTION_DATA_FIELDS = List.of("list", "records", "rows", "items", "children");

    private final RestClient restClient;

    private final String apiKey;

    private final PrivateKey privateKey;

    private final ObjectMapper objectMapper;

    public ZyplayerOpenApiClient(String baseUrl, String apiKey, String privateKey, ObjectMapper objectMapper) {
        this(RestClient.builder().baseUrl(baseUrl).build(), apiKey, privateKey, objectMapper);
    }

    public ZyplayerOpenApiClient(RestClient restClient, String apiKey, String privateKey, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.privateKey = parsePrivateKey(privateKey);
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ZyplayerSpace> listSpaces() {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ZyplayerSpace.class);
        return post("/openApi/v1/space/list", Map.of(), type);
    }

    @Override
    public List<ZyplayerSpaceGroup> listSpaceGroups() {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ZyplayerSpaceGroup.class);
        return post("/openApi/v1/spaceGroup/list", Map.of(), type);
    }

    @Override
    public ZyplayerSpaceGroup updateSpaceGroup(Map<String, Object> payload) {
        return post("/openApi/v1/spaceGroup/update", payload, objectMapper.constructType(ZyplayerSpaceGroup.class));
    }

    @Override
    public ZyplayerSpace updateSpace(Map<String, Object> payload) {
        return post("/openApi/v1/space/update", payload, objectMapper.constructType(ZyplayerSpace.class));
    }

    @Override
    public List<ZyplayerSpaceVersion> listSpaceVersions(Long spaceId) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ZyplayerSpaceVersion.class);
        return post("/openApi/v1/space/version/list", Map.of("spaceId", spaceId), type);
    }

    @Override
    public ZyplayerSpaceVersion createSpaceVersion(Map<String, Object> payload) {
        return post("/openApi/v1/space/createVersion", payload, objectMapper.constructType(ZyplayerSpaceVersion.class));
    }

    @Override
    public List<ZyplayerPage> listPages(Long spaceId) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, ZyplayerPage.class);
        return post("/openApi/v1/space/page/list", Map.of("spaceId", spaceId), type);
    }

    @Override
    public ZyplayerPageContent pageDetail(Long spaceId, Long pageId) {
        return post("/openApi/v1/space/page/detail",
                Map.of("spaceId", spaceId, "id", pageId),
                objectMapper.constructType(ZyplayerPageContent.class));
    }

    @Override
    public ZyplayerPage updatePage(Map<String, Object> payload) {
        return post("/openApi/v1/space/page/update", payload, objectMapper.constructType(ZyplayerPage.class));
    }

    @Override
    public void deletePage(Long spaceId, Long pageId) {
        post("/openApi/v1/space/page/delete", Map.of("spaceId", spaceId, "pageId", pageId, "delFlag", 1),
                objectMapper.constructType(JsonNode.class));
    }

    @Override
    public void releasePage(Long pageId) {
        post("/openApi/v1/space/page/release", Map.of("id", pageId), objectMapper.constructType(JsonNode.class));
    }

    private <T> T post(String path, Map<String, Object> payload, JavaType responseType) {
        try {
            String content = objectMapper.writeValueAsString(withSalt(payload));
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("key", apiKey);
            form.add("signature", sign(content));
            form.add("content", content);

            String response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            int errCode = root.path("errCode").asInt(SUCCESS_CODE);
            if (errCode != SUCCESS_CODE) {
                throw new ZyplayerOpenApiException(root.path("errMsg").asText("zyplayer open-api error: " + errCode));
            }
            JsonNode data = root.get("data");
            if (data == null || data.isNull()) {
                data = objectMapper.createObjectNode();
            }
            JsonNode unwrappedData = unwrapData(data, responseType);
            if (responseType.isCollectionLikeType() && !unwrappedData.isArray()) {
                throw new ZyplayerOpenApiException("unsupported zyplayer collection data shape: " + summarizeDataShape(data));
            }
            return objectMapper.convertValue(unwrappedData, responseType);
        } catch (ZyplayerOpenApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ZyplayerOpenApiException("zyplayer open-api request failed: " + path, e);
        }
    }

    private JsonNode unwrapData(JsonNode data, JavaType responseType) {
        if (!responseType.isCollectionLikeType() || !data.isObject()) {
            return data;
        }
        if (data.isEmpty()) {
            return objectMapper.createArrayNode();
        }
        for (String field : COLLECTION_DATA_FIELDS) {
            JsonNode value = data.get(field);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        JsonNode onlyArrayValue = null;
        int arrayFieldCount = 0;
        for (var iterator = data.properties().iterator(); iterator.hasNext(); ) {
            JsonNode value = iterator.next().getValue();
            if (value.isArray()) {
                onlyArrayValue = value;
                arrayFieldCount++;
            }
        }
        if (arrayFieldCount == 1) {
            return onlyArrayValue;
        }
        return data;
    }

    private String summarizeDataShape(JsonNode data) {
        if (!data.isObject()) {
            return data.getNodeType().name();
        }
        StringBuilder builder = new StringBuilder("{");
        for (var iterator = data.properties().iterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            builder.append(entry.getKey()).append(':').append(entry.getValue().getNodeType().name());
            if (iterator.hasNext()) {
                builder.append(',');
            }
        }
        return builder.append('}').toString();
    }

    private Map<String, Object> withSalt(Map<String, Object> payload) {
        Map<String, Object> content = new LinkedHashMap<>(payload);
        content.putIfAbsent("salt", UUID.randomUUID().toString());
        return content;
    }

    private String sign(String content) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        return toHex(signature.sign());
    }

    private String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }

    private PrivateKey parsePrivateKey(String privateKey) {
        try {
            String normalized = privateKey == null ? "" : privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            if (!StringUtils.hasText(normalized)) {
                throw new IllegalArgumentException("private key is blank");
            }
            byte[] bytes = Base64.getDecoder().decode(normalized);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new ZyplayerOpenApiException("invalid zyplayer private key", e);
        }
    }
}
