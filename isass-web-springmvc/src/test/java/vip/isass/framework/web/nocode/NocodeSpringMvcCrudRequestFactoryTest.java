package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.access.NocodeAccessRequest;
import vip.isass.framework.nocode.v3.access.NocodeCrudAccessRequests;
import vip.isass.framework.nocode.v3.access.NocodeDeleteOptions;
import vip.isass.framework.nocode.v3.access.NocodeFetchOptions;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeSpringMvcCrudRequestFactoryTest {

    private final NocodeSpringMvcCrudRequestFactory factory = new NocodeSpringMvcCrudRequestFactory();

    @Test
    void createsFindByIdRequestFromRoute() {
        NocodeAccessRequest request = factory.create(
                NocodeSpringMvcCrudRoute.defaultRoutes().get(2),
                NocodeSpringMvcCrudRequestArguments.byId("attachment", "1001", NocodeFetchOptions.include("items"))
                        .withReturnType(String.class)
        );

        assertThat(request.entityName()).isEqualTo("attachment");
        assertThat(request.operationName()).isEqualTo(NocodeCrudOperation.FIND_BY_ID.getOperationName());
        assertThat(request.arguments())
                .containsEntry(NocodeCrudAccessRequests.ARG_ID, "1001")
                .containsKey(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS);
        assertThat(request.returnType()).isEqualTo(String.class);
    }

    @Test
    void createsListAndPageRequestsWithCriteria() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .where("name", "demo")
                .page(1, 20)
                .build();

        NocodeAccessRequest list = factory.create(
                NocodeCrudOperation.LIST,
                NocodeSpringMvcCrudRequestArguments.query("attachment", criteria)
        );
        NocodeAccessRequest page = factory.create(
                NocodeCrudOperation.PAGE,
                NocodeSpringMvcCrudRequestArguments.query("attachment", criteria, NocodeFetchOptions.include())
        );

        assertThat(list.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_CRITERIA, criteria);
        assertThat(page.arguments())
                .containsEntry(NocodeCrudAccessRequests.ARG_CRITERIA, criteria)
                .containsKey(NocodeCrudAccessRequests.ARG_FETCH_OPTIONS);
    }

    @Test
    void createsWriteRequests() {
        Object body = new Object();

        NocodeAccessRequest save = factory.create(
                NocodeCrudOperation.SAVE,
                NocodeSpringMvcCrudRequestArguments.body("attachment", body)
        );
        NocodeAccessRequest update = factory.create(
                NocodeCrudOperation.UPDATE_BY_ID,
                NocodeSpringMvcCrudRequestArguments.bodyById("attachment", 1001L, body)
        );

        assertThat(save.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_BODY, body);
        assertThat(update.arguments())
                .containsEntry(NocodeCrudAccessRequests.ARG_ID, 1001L)
                .containsEntry(NocodeCrudAccessRequests.ARG_BODY, body);
    }

    @Test
    void createsDeleteRequestWithOptions() {
        NocodeDeleteOptions options = NocodeDeleteOptions.associated("items");

        NocodeAccessRequest request = factory.create(
                NocodeCrudOperation.DELETE_BY_ID,
                NocodeSpringMvcCrudRequestArguments.delete("attachment", 1001L, options)
        );

        assertThat(request.arguments())
                .containsEntry(NocodeCrudAccessRequests.ARG_ID, 1001L)
                .containsEntry(NocodeCrudAccessRequests.ARG_DELETE_OPTIONS, options);
    }
}
