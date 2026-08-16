// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.common.page.Page;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
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
                method, List.of(), method.getGenericReturnType(), true);
        ServiceDefinition service = new ServiceDefinition(
                "schema-test-service", "sample", "sampleGroup", TestEntrypoint.class,
                List.of(operation), true);
        MockEnvironment environment = new MockEnvironment()
                .withProperty("isass.boot.microservice.enabled", "true")
                .withProperty("spring.application.name", "schema-test-service");

        String document = new OpenApiDocumentAssembler(
                new ObjectMapper(), new SingleServiceRegistry(service), environment).assemble();

        assertThat(document)
                .contains("vip_isass_framework_common_page_Page__"
                        + "vip_isass_framework_web_servicedocs_OpenApiDocumentAssemblerSchemaTest_TestEntity")
                .contains("vip_isass_framework_web_servicedocs_OpenApiDocumentAssemblerSchemaTest_TestEntity")
                .contains("\"records\"")
                .doesNotContain("com_baomidou_mybatisplus_core_metadata_IPage");
    }

    private interface TestEntrypoint extends IEntrypoint {
        Page<TestEntity> page();
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
}
