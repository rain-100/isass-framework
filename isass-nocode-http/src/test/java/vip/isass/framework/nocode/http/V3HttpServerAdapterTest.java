package vip.isass.framework.nocode.http;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.criteria.field.IV3IdCriteria;
import vip.isass.framework.nocode.v3.criteria.impl.type.V3FullTypeCriteria;
import vip.isass.framework.nocode.v3.entity.IV3IdEntity;
import vip.isass.framework.nocode.v3.contract.V3ContractRegistry;
import vip.isass.framework.nocode.v3.contract.V3HttpMethod;
import vip.isass.framework.nocode.v3.contract.V3OperationContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterContract;
import vip.isass.framework.nocode.v3.contract.V3ParameterSource;
import vip.isass.framework.nocode.v3.contract.V3ServiceContract;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class V3HttpServerAdapterTest {

    public interface CustomApi {
        String findAvailableIcons(Long tenantId);
    }

    public interface CriteriaApi {
        String firstCriteriaConditionValueType(TestIconCriteria criteria);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void oneAdapterInvokesCustomMethodForEntity() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CustomApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("icon");
        when(((CustomApi) service).findAvailableIcons(9L)).thenReturn("icons");

        V3OperationContract operation = new V3OperationContract(
                "findAvailableIcons", V3HttpMethod.GET, "/available/{tenantId}",
                501, true,
                List.of(new V3ParameterContract(
                        "tenantId", Long.class.getName(), V3ParameterSource.PATH,
                        true, "租户 ID")),
                String.class.getName(), "查询可用图标");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "icon", CustomApi.class.getName(),
                "example.Icon", "example.IconCriteria", List.of(operation));
        V3HttpServerAdapter adapter = new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)),
                new V3ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/v3/available/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("icons"));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void queryParametersBindToCriteriaDefaultIdSetter() throws Exception {
        IV3Service service = mock(IV3Service.class,
                org.mockito.Mockito.withSettings().extraInterfaces(CriteriaApi.class));
        when(service.serviceName()).thenReturn("attachment-service");
        when(service.entityName()).thenReturn("icon");
        when(((CriteriaApi) service).firstCriteriaConditionValueType(any(TestIconCriteria.class)))
                .thenAnswer(invocation -> {
                    TestIconCriteria criteria = invocation.getArgument(0, TestIconCriteria.class);
                    if (criteria.getWhereConditions().isEmpty()) {
                        return "empty";
                    }
                    return criteria.getWhereConditions().getFirst().getValue().getClass().getName();
                });

        V3OperationContract operation = new V3OperationContract(
                "firstCriteriaConditionValueType", V3HttpMethod.GET, "/criteria",
                301, true,
                List.of(new V3ParameterContract(
                        "criteria", TestIconCriteria.class.getName(), V3ParameterSource.QUERY,
                        false, "查询条件")),
                Integer.class.getName(), "按条件查询");
        V3ServiceContract contract = new V3ServiceContract(
                "attachment-service", "icon", CriteriaApi.class.getName(),
                TestIcon.class.getName(), TestIconCriteria.class.getName(), List.of(operation));
        V3HttpServerAdapter adapter = new V3HttpServerAdapter(
                new V3ContractRegistry(List.of(contract)),
                new V3ServiceRegistry(List.of(service)),
                new ObjectMapper());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(adapter).build();

        mvc.perform(get("/attachment-service/icon/v3/criteria?id=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(Long.class.getName()));
    }

    public static class TestIcon implements IV3IdEntity<Long, TestIcon> {

        private Long id;

        @Override
        public Long getId() {
            return id;
        }

        @Override
        public void setId(Long id) {
            this.id = id;
        }

        @Override
        public TestIcon randomEntity() {
            return this;
        }
    }

    public static class TestIconCriteria
            extends V3FullTypeCriteria<TestIcon, TestIconCriteria>
            implements IV3IdCriteria<Long, TestIcon, TestIconCriteria> {
    }
}
