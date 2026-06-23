package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vip.isass.framework.nocode.v3.access.NocodeAccessHandler;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperationExecutor;
import vip.isass.framework.nocode.v3.routing.NocodeOperationProvider;
import vip.isass.framework.nocode.v3.routing.NocodeProviderType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NocodeCrudControllerMockMvcTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NocodeAccessHandler accessHandler = new NocodeAccessHandler(
                new NocodeOperationExecutor(List.of(new TestProvider()), List.of()));
        NocodeSpringMvcCrudEndpointInvoker invoker = new NocodeSpringMvcCrudEndpointInvoker(accessHandler);
        NocodeCrudController controller = new NocodeCrudController(
                invoker, new NocodeSpringMvcQueryCriteriaParser());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void findByIdReturnsResult() throws Exception {
        mockMvc.perform(get("/nocode/testEntity/42"))
                .andExpect(status().isOk())
                .andExpect(content().string("testEntity:findById:42"));
    }

    @Test
    void listReturnsResult() throws Exception {
        mockMvc.perform(get("/nocode/testEntity"))
                .andExpect(status().isOk())
                .andExpect(content().string("testEntity:list:"));
    }

    @Test
    void pageReturnsResult() throws Exception {
        mockMvc.perform(get("/nocode/testEntity/page")
                        .param("pageNumber", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string("testEntity:page:"));
    }

    @Test
    void saveReturnsResult() throws Exception {
        mockMvc.perform(post("/nocode/testEntity")
                        .contentType("application/json")
                        .content("{\"name\":\"test\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReturnsResult() throws Exception {
        mockMvc.perform(delete("/nocode/testEntity/99"))
                .andExpect(status().isOk())
                .andExpect(content().string("testEntity:deleteById:99"));
    }

    static class TestProvider implements NocodeOperationProvider<Object> {

        @Override
        public NocodeProviderType getProviderType() {
            return NocodeProviderType.LOCAL;
        }

        @Override
        public boolean supports(NocodeOperation operation) {
            return "testEntity".equals(operation.entityName());
        }

        @Override
        public Object invoke(NocodeOperation operation) {
            return operation.entityName() + ":" + operation.operationName() + ":" +
                    operation.arguments().getOrDefault("id", "");
        }
    }
}
