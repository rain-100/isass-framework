package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMethod;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeSpringMvcCrudRouteTest {

    @Test
    void buildsDefaultCrudRoutes() {
        List<NocodeSpringMvcCrudRoute> routes = NocodeSpringMvcCrudRoute.defaultRoutes();

        assertThat(routes)
                .extracting(NocodeSpringMvcCrudRoute::operation)
                .containsExactly(
                        NocodeCrudOperation.LIST,
                        NocodeCrudOperation.PAGE,
                        NocodeCrudOperation.FIND_BY_ID,
                        NocodeCrudOperation.SAVE,
                        NocodeCrudOperation.UPDATE_BY_ID,
                        NocodeCrudOperation.DELETE_BY_ID
                );
        assertThat(routes.get(0).method()).isEqualTo(RequestMethod.GET);
        assertThat(routes.get(0).pathPattern()).isEqualTo("/nocode/{entityName}");
        assertThat(routes.get(2).pathVariables()).containsExactly("entityName", "id");
    }

    @Test
    void normalizesBasePath() {
        List<NocodeSpringMvcCrudRoute> routes = NocodeSpringMvcCrudRoute.routes("api/nocode/");

        assertThat(routes.get(0).pathPattern()).isEqualTo("/api/nocode/{entityName}");
    }
}
