// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterDefinition;
import vip.isass.framework.entrypoint.metadata.ParameterSource;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentAssemblerSchemaTest {

    @Test
    void resolvesPageGenericRecordTypeWithoutIntrospectingOrmInterfaces() throws Exception {
        Method method = TestEntrypoint.class.getMethod("page");
        OperationDefinition operation = new OperationDefinition(
                "page", "查-分页列表", "分页查询", 1, HttpMethod.GET,
                UrlAccessSecurityStrategy.ROLE,
                method, List.of(), method.getGenericReturnType(), true);
        ServiceDefinition service = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", "样片组", TestEntrypoint.class,
                List.of(operation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");

        String document = new OpenApiDocumentAssembler(
                new ObjectMapper(), new SingleServiceRegistry(service), environment).assemble();
        var parsed = new ObjectMapper().readTree(document);

        assertThat(document)
                .contains("vip_isass_framework_common_page_Page__"
                        + "vip_isass_framework_web_servicedocs_OpenApiDocumentAssemblerSchemaTest_TestEntity")
                .contains("vip_isass_framework_web_servicedocs_OpenApiDocumentAssemblerSchemaTest_TestEntity")
                .contains("\"records\"")
                .doesNotContain("com_baomidou_mybatisplus_core_metadata_IPage");
        assertThat(parsed.path("paths").has("/{service}/nocode/{entity}/page"))
                .as(parsed.path("paths").toPrettyString()).isTrue();
        assertThat(parsed.path("paths").has("/schema-test-service/nocode/sample/sampleGroup/page"))
                .isFalse();
        assertThat(parsed.path("paths").has("schema-test-service")).isFalse();
        assertThat(parsed.path("paths")
                .path("/{service}/nocode/{entity}/page")
                .path("get").path("tags").get(0).asText()).isEqualTo("零代码接口");
        assertThat(parsed.path("paths")
                .path("/{service}/nocode/{entity}/page")
                .path("get").path("parameters").toString())
                .contains("\"name\":\"service\"", "\"name\":\"entity\"");
    }

    @Test
    void describesCollectionQueryParametersAsCommaSeparated() throws Exception {
        Method method = TestEntrypoint.class.getMethod("search", List.class);
        OperationDefinition operation = new OperationDefinition(
                "search", "查询", "条件查询", 1, HttpMethod.GET,
                UrlAccessSecurityStrategy.ROLE, method,
                List.of(new ParameterDefinition(0, "idIn", ParameterSource.QUERY,
                        method.getGenericParameterTypes()[0], false)),
                method.getGenericReturnType(), true);
        ServiceDefinition service = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", "样片组", TestEntrypoint.class,
                List.of(operation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");
        ObjectMapper objectMapper = new ObjectMapper();

        var document = objectMapper.readTree(new OpenApiDocumentAssembler(
                objectMapper, new SingleServiceRegistry(service), environment).assemble());
        tools.jackson.databind.JsonNode parameter = null;
        for (var parameters : document.path("paths").findValues("parameters")) {
            for (var candidate : parameters) {
                if (candidate.path("name").asText().equals("idIn")) {
                    parameter = candidate;
                    break;
                }
            }
        }

        assertThat(parameter).isNotNull();
        assertThat(parameter.path("style").asText()).isEqualTo("form");
        assertThat(parameter.path("explode").asBoolean()).isFalse();
    }

    @Test
    void groupsOnlyCustomOperationsByBusinessResource() throws Exception {
        Method method = TestEntrypoint.class.getMethod("all");
        OperationDefinition operation = new OperationDefinition(
                "all", "查询全部", "自定义业务查询", 1, HttpMethod.GET,
                UrlAccessSecurityStrategy.ROLE, method, List.of(),
                method.getGenericReturnType(), false);
        ServiceDefinition service = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", "样片组", TestEntrypoint.class,
                List.of(operation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");
        ObjectMapper objectMapper = new ObjectMapper();

        var document = objectMapper.readTree(new OpenApiDocumentAssembler(
                objectMapper, new SingleServiceRegistry(service), environment).assemble());

        assertThat(document.path("paths")
                .path("/schema-test-service/sample/sampleGroup/all")
                .path("get").path("tags").get(0).asText()).isEqualTo("样片组");
    }

    @Test
    void ordersCustomEntrypointTagsByServiceDisplayOrder() throws Exception {
        Method firstMethod = TestEntrypoint.class.getMethod("all");
        Method secondMethod = TestEntrypoint.class.getMethod("all");
        OperationDefinition firstOperation = customOperation(firstMethod, "first");
        OperationDefinition secondOperation = customOperation(secondMethod, "second");
        ServiceDefinition firstService = new ServiceDefinition(
                "schema-test-service", "sample", "first", "首个分组", 20,
                TestEntrypoint.class, List.of(firstOperation), true);
        ServiceDefinition secondService = new ServiceDefinition(
                "schema-test-service", "sample", "second", "第二个分组", 10,
                OtherEntrypoint.class, List.of(secondOperation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");
        ObjectMapper objectMapper = new ObjectMapper();

        var document = objectMapper.readTree(new OpenApiDocumentAssembler(
                objectMapper, new CollectionServiceRegistry(List.of(firstService, secondService)),
                environment).assemble());

        assertThat(document.path("tags").get(0).path("name").asText()).isEqualTo("第二个分组");
        assertThat(document.path("tags").get(0).path("x-order").asInt()).isEqualTo(10);
        assertThat(document.path("tags").get(1).path("name").asText()).isEqualTo("首个分组");
    }

    @Test
    void prioritizesNocodeTagWhenDisplayOrderMatchesCustomTag() throws Exception {
        Method customMethod = TestEntrypoint.class.getMethod("all");
        Method nocodeMethod = TestEntrypoint.class.getMethod("page");
        ServiceDefinition customService = new ServiceDefinition(
                "schema-test-service", "sample", "custom", "自定义分组", 1,
                TestEntrypoint.class, List.of(customOperation(customMethod, "all")), true);
        ServiceDefinition nocodeService = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", "样片组",
                TestEntrypoint.class, List.of(nocodePage(nocodeMethod)), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");
        ObjectMapper objectMapper = new ObjectMapper();

        var document = objectMapper.readTree(new OpenApiDocumentAssembler(
                objectMapper, new CollectionServiceRegistry(List.of(customService, nocodeService)),
                environment).assemble());

        assertThat(document.path("tags").get(0).path("name").asText()).isEqualTo("零代码接口");
        assertThat(document.path("tags").get(0).path("x-order").asInt()).isEqualTo(1);
        assertThat(document.path("tags").get(1).path("name").asText()).isEqualTo("自定义分组");
    }

    private OperationDefinition customOperation(Method method, String operationName) {
        return new OperationDefinition(
                operationName, operationName, "自定义业务查询", 1, HttpMethod.GET,
                UrlAccessSecurityStrategy.ROLE, method, List.of(),
                method.getGenericReturnType(), false);
    }

    @Test
    void projectsEachNocodeOperationOnceWithDynamicServiceAndEntityOptions() throws Exception {
        Method samplePage = TestEntrypoint.class.getMethod("page");
        Method facePage = OtherEntrypoint.class.getMethod("page");
        OperationDefinition sampleOperation = nocodePage(samplePage);
        OperationDefinition faceOperation = nocodePage(facePage);
        ServiceDefinition sampleService = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", "样片组", TestEntrypoint.class,
                List.of(sampleOperation), true);
        ServiceDefinition faceService = new ServiceDefinition(
                "schema-test-service", "sample", "modelFace", "模特脸", OtherEntrypoint.class,
                List.of(faceOperation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");
        ObjectMapper objectMapper = new ObjectMapper();

        var document = objectMapper.readTree(new OpenApiDocumentAssembler(
                objectMapper, new CollectionServiceRegistry(List.of(sampleService, faceService)),
                environment).assemble());
        var api = document.path("paths").path("/{service}/nocode/{entity}/page").path("get");

        assertThat(document.path("tags").get(0).path("name").asText()).isEqualTo("零代码接口");
        assertThat(document.path("tags").get(0).path("x-order").asInt()).isEqualTo(1);
        assertThat(document.path("paths").size()).isEqualTo(1);
        assertThat(api.path("x-isass-service-entities").path("schema-test-service").toString())
                .contains("sample/sampleGroup", "sample/modelFace");
        assertThat(api.path("x-isass-entity-options").path("sampleGroup").asText())
                .isEqualTo("sample/sampleGroup");
        assertThat(api.path("x-isass-entity-options").path("modelFace").asText())
                .isEqualTo("sample/modelFace");
        assertThat(api.path("responses").path("200").path("content").path("application/json")
                .path("schema").path("oneOf").size()).isEqualTo(2);
    }

    private OperationDefinition nocodePage(Method method) {
        return new OperationDefinition(
                "page", "查-分页列表", "分页查询", 301, HttpMethod.GET,
                UrlAccessSecurityStrategy.ROLE,
                method, List.of(), method.getGenericReturnType(), true);
    }

    private interface TestEntrypoint extends IEntrypoint {
        Page<TestEntity> page();

        List<TestEntity> search(List<Long> idIn);

        List<TestEntity> all();
    }

    private interface OtherEntrypoint extends IEntrypoint {
        Page<OtherEntity> page();
    }

    private static final class TestEntity {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    private static final class OtherEntity {
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    private record SingleServiceRegistry(ServiceDefinition service) implements ServiceDefinitionRegistry {
        @Override
        public Collection<ServiceDefinition> all() {
            return List.of(service);
        }

        @Override
        public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
            return service.serviceName().equals(serviceName)
                    && service.contextName().equals(contextName)
                    && service.resourceName().equals(resourceName)
                    ? Optional.of(service) : Optional.empty();
        }
    }

    private record CollectionServiceRegistry(List<ServiceDefinition> services)
            implements ServiceDefinitionRegistry {
        @Override
        public Collection<ServiceDefinition> all() {
            return services;
        }

        @Override
        public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
            return services.stream().filter(service -> service.serviceName().equals(serviceName)
                            && service.contextName().equals(contextName)
                            && service.resourceName().equals(resourceName))
                    .findFirst();
        }
    }
}
