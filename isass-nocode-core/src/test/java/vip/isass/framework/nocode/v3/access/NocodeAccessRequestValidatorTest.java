package vip.isass.framework.nocode.v3.access;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeAccessRequestValidatorTest {

    private final NocodeAccessRequestValidator validator = new NocodeAccessRequestValidator();

    @Test
    void acceptsStandardCrudRequestWithRequiredArguments() {
        NocodeAccessRequest request = NocodeCrudAccessRequests.findById("attachment", 1L, Object.class);

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingRequiredArgumentForStandardOperation() {
        NocodeAccessRequest request = new NocodeAccessRequest(
                "attachment",
                NocodeCrudOperation.FIND_BY_ID.getOperationName(),
                Map.of(),
                Object.class,
                null
        );

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(NocodeAccessValidationException.class)
                .hasMessageContaining("Missing required argument 'id'");
    }

    @Test
    void rejectsUnsupportedArgumentForStandardOperation() {
        NocodeAccessRequest request = new NocodeAccessRequest(
                "attachment",
                NocodeCrudOperation.DELETE_BY_ID.getOperationName(),
                Map.of(NocodeCrudAccessRequests.ARG_ID, 1L, "unknown", true),
                Object.class,
                null
        );

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(NocodeAccessValidationException.class)
                .hasMessageContaining("Unsupported argument 'unknown'");
    }

    @Test
    void ignoresCustomOperationWithoutStandardDefinition() {
        NocodeAccessRequest request = new NocodeAccessRequest(
                "attachment",
                "customAction",
                Map.of("anything", true),
                Object.class,
                null
        );

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }
}
