package vip.isass.framework.apidoc.zyplayer.openapi;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerSyncDocument;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerOpenApiDocumentConverterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void convertsGetOperationToZyplayerApiDocument() throws Exception {
        ZyplayerOpenApiDocumentConverter converter = new ZyplayerOpenApiDocumentConverter(objectMapper);

        List<ZyplayerSyncDocument> documents = converter.convert("""
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/attachment-service/fileBrowse": {
                      "get": {
                        "summary": "文件列表",
                        "description": "基于相对路径",
                        "parameters": [
                          {"name": "path", "in": "query", "required": false, "description": "目标文件夹", "schema": {"type": "string"}},
                          {"name": "Authorization", "in": "header", "required": false, "description": "访问令牌", "schema": {"type": "string"}}
                        ],
                        "responses": {
                          "200": {
                            "description": "OK",
                            "content": {
                              "application/json": {
                                "schema": {
                                  "type": "object",
                                  "properties": {
                                    "success": {"type": "boolean", "description": "是否成功"},
                                    "data": {"type": "array", "items": {"type": "string"}}
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                  }
                }
                """, "http://127.0.0.1:20320");

        assertThat(documents).singleElement().satisfies(document -> {
            assertThat(document.id()).isEqualTo("api/get/attachment-service/fileBrowse");
            assertThat(document.title()).isEqualTo("GET /attachment-service/fileBrowse 文件列表");
            assertThat(document.editorType()).isEqualTo(6);
            JsonNode content = read(document.content());
            assertThat(content.path("method").asText()).isEqualTo("get");
            assertThat(content.path("apiUrl").asText()).isEqualTo("http://127.0.0.1:20320/attachment-service/fileBrowse");
            assertThat(content.path("description").asText()).isEqualTo("基于相对路径");
            assertThat(content.path("urlParams").get(0).path("name").asText()).isEqualTo("path");
            assertThat(content.path("urlParams").get(0).path("desc").asText()).isEqualTo("目标文件夹");
            assertThat(content.path("headerParams").get(0).path("name").asText()).isEqualTo("Authorization");
            assertThat(content.path("response").get(0).path("statusCode").asInt()).isEqualTo(200);
            assertThat(content.path("response").get(0).path("data").path("children").get(0).path("name").asText()).isEqualTo("success");
        });
    }

    @Test
    void convertsJsonRequestBodyToRawBodyObject() throws Exception {
        ZyplayerOpenApiDocumentConverter converter = new ZyplayerOpenApiDocumentConverter(objectMapper);

        List<ZyplayerSyncDocument> documents = converter.convert("""
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/attachment-service/fileSystem/mkdir": {
                      "post": {
                        "operationId": "mkdir",
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "path": {"type": "string", "description": "目录路径"}
                                },
                                "required": ["path"]
                              }
                            }
                          }
                        },
                        "responses": {"200": {"description": "OK"}}
                      }
                    }
                  }
                }
                """, "http://127.0.0.1:20320");

        JsonNode content = read(documents.get(0).content());

        assertThat(content.path("method").asText()).isEqualTo("post");
        assertThat(content.path("bodyParamType").asText()).isEqualTo("row");
        assertThat(content.path("bodyConsumesType").asText()).isEqualTo("json");
        assertThat(content.path("bodyParamObj").path("children").get(0).path("name").asText()).isEqualTo("path");
        assertThat(content.path("bodyParamRow").asText()).contains("\"path\"");
    }

    @Test
    void usesFirstOperationTagAsApiFolderGroup() {
        ZyplayerOpenApiDocumentConverter converter = new ZyplayerOpenApiDocumentConverter(objectMapper);

        List<ZyplayerSyncDocument> documents = converter.convert("""
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/attachment-service/upload": {
                      "post": {
                        "summary": "上传附件",
                        "tags": ["附件上传"],
                        "responses": {"200": {"description": "OK"}}
                      }
                    }
                  }
                }
                """, "http://127.0.0.1:20320");

        assertThat(documents).singleElement().satisfies(document ->
                assertThat(document.folderPath()).containsExactly("api接口", "附件上传"));
    }

    @Test
    void skipsOperationsMatchedByExcludeRules() {
        ZyplayerOpenApiDocumentConverter converter = new ZyplayerOpenApiDocumentConverter(objectMapper,
                new ZyplayerOpenApiExcludeRules(List.of("GET /error"), List.of("/actuator/**"), List.of()));

        List<ZyplayerSyncDocument> documents = converter.convert("""
                {
                  "openapi": "3.1.0",
                  "paths": {
                    "/error": {
                      "get": {"summary": "错误页", "responses": {"200": {"description": "OK"}}},
                      "post": {"summary": "错误页提交", "responses": {"200": {"description": "OK"}}}
                    },
                    "/actuator/health": {
                      "get": {"summary": "健康检查", "responses": {"200": {"description": "OK"}}}
                    },
                    "/attachment-service/fileBrowse": {
                      "get": {"summary": "文件列表", "responses": {"200": {"description": "OK"}}}
                    }
                  }
                }
                """, "http://127.0.0.1:20320");

        assertThat(documents).extracting(ZyplayerSyncDocument::title)
                .containsExactly(
                        "POST /error 错误页提交",
                        "GET /attachment-service/fileBrowse 文件列表");
    }

    private JsonNode read(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
