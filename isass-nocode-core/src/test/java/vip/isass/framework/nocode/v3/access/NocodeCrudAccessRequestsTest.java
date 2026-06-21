package vip.isass.framework.nocode.v3.access;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;
import vip.isass.framework.nocode.v3.routing.NocodeRouteMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class NocodeCrudAccessRequestsTest {

    @Test
    void createsFindByIdRequestWithStableArgumentName() {
        NocodeAccessRequest request = NocodeCrudAccessRequests.findById("attachment", 1001L, String.class);

        assertThat(request.entityName()).isEqualTo("attachment");
        assertThat(request.operationName()).isEqualTo(NocodeCrudOperation.FIND_BY_ID.getOperationName());
        assertThat(request.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_ID, 1001L);
        assertThat(request.returnType()).isEqualTo(String.class);
        assertThat(request.routeMode()).isEqualTo(NocodeRouteMode.AUTO);
    }

    @Test
    void createsPageRequestWithCriteriaArgument() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .where("name", "demo")
                .build();

        NocodeAccessRequest request = NocodeCrudAccessRequests.page("attachment", criteria, Object.class);

        assertThat(request.operationName()).isEqualTo(NocodeCrudOperation.PAGE.getOperationName());
        assertThat(request.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_CRITERIA, criteria);
    }

    @Test
    void createsDeleteByIdRequestWithCascadeDeleteOptions() {
        NocodeDeleteOptions options = NocodeDeleteOptions.cascade("attachmentItems");

        NocodeAccessRequest request = NocodeCrudAccessRequests.deleteById("attachment", 1001L, options, Boolean.class);

        assertThat(request.operationName()).isEqualTo(NocodeCrudOperation.DELETE_BY_ID.getOperationName());
        assertThat(request.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_ID, 1001L);
        assertThat(request.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_DELETE_OPTIONS, options);
        assertThat(options.cascadeDelete()).isTrue();
        assertThat(options.associatedDelete()).isFalse();
        assertThat(options.relationNames()).containsExactly("attachmentItems");
    }

    @Test
    void createsDeleteByIdRequestWithAssociatedDeleteOptions() {
        NocodeDeleteOptions options = NocodeDeleteOptions.associated("attachmentTags");

        NocodeAccessRequest request = NocodeCrudAccessRequests.deleteById("attachment", 1001L, options, Boolean.class);

        assertThat(request.arguments()).containsEntry(NocodeCrudAccessRequests.ARG_DELETE_OPTIONS, options);
        assertThat(options.cascadeDelete()).isFalse();
        assertThat(options.associatedDelete()).isTrue();
        assertThat(options.hasRelationFilter()).isTrue();
    }

    @Test
    void rejectsBlankEntityName() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> NocodeCrudAccessRequests.deleteById(" ", 1L, Boolean.class));
    }
}
