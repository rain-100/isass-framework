package vip.isass.framework.nocode.v3.validation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeEntityRegistry;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.operation.NocodeOperation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeCrudValidationInterceptorTest {

    private final NocodeEntityDefinition entityDefinition = NocodeEntityDefinition.builder("attachment", Object.class)
            .field(NocodeFieldDefinition.builder("name", String.class)
                    .constraint(NocodeFieldConstraint.notBlank(NocodeValidationGroup.CREATE))
                    .constraint(NocodeFieldConstraint.size(2, 8, NocodeValidationGroup.UPDATE))
                    .build())
            .build();

    @Test
    void validatesSaveWithCreateGroup() {
        NocodeCrudValidationInterceptor interceptor = new NocodeCrudValidationInterceptor(
                new NocodeEntityRegistry(List.of(entityDefinition))
        );
        NocodeOperation operation = new NocodeOperation(
                "attachment",
                NocodeCrudOperation.SAVE.getOperationName(),
                Map.of("body", Map.of("name", " ")),
                Object.class
        );

        assertThatThrownBy(() -> interceptor.intercept(operation, next -> "unused"))
                .isInstanceOf(NocodeEntityValidationException.class)
                .hasMessage("name: must not be blank");
    }

    @Test
    void validatesUpdateWithUpdateGroup() {
        NocodeCrudValidationInterceptor interceptor = new NocodeCrudValidationInterceptor(
                new NocodeEntityRegistry(List.of(entityDefinition))
        );
        NocodeOperation operation = new NocodeOperation(
                "attachment",
                NocodeCrudOperation.UPDATE_BY_ID.getOperationName(),
                Map.of("body", Map.of("name", "a")),
                Object.class
        );

        assertThatThrownBy(() -> interceptor.intercept(operation, next -> "unused"))
                .isInstanceOf(NocodeEntityValidationException.class)
                .hasMessage("name: size must be between 2 and 8");
    }

    @Test
    void continuesWhenValidationPasses() {
        NocodeCrudValidationInterceptor interceptor = new NocodeCrudValidationInterceptor(
                new NocodeEntityRegistry(List.of(entityDefinition))
        );
        NocodeOperation operation = new NocodeOperation(
                "attachment",
                NocodeCrudOperation.SAVE.getOperationName(),
                Map.of("body", Map.of("name", "demo")),
                Object.class
        );

        String result = interceptor.intercept(operation, next -> "ok");

        assertThat(result).isEqualTo("ok");
    }
}
