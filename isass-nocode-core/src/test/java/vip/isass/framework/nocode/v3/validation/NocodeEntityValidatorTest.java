package vip.isass.framework.nocode.v3.validation;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeEntityValidatorTest {

    private final NocodeEntityDefinition entityDefinition = NocodeEntityDefinition.builder("attachment", Object.class)
            .field(NocodeFieldDefinition.builder("name", String.class)
                    .constraint(NocodeFieldConstraint.notBlank(NocodeValidationGroup.CREATE))
                    .constraint(NocodeFieldConstraint.size(2, 8, NocodeValidationGroup.CREATE, NocodeValidationGroup.UPDATE))
                    .build())
            .field(NocodeFieldDefinition.builder("storagePath", String.class)
                    .constraint(NocodeFieldConstraint.notNull())
                    .build())
            .build();

    @Test
    void validatesCreateGroupConstraints() {
        NocodeEntityValidator validator = new NocodeEntityValidator();

        assertThat(validator.validate(
                entityDefinition,
                Map.of("name", " ", "storagePath", "/data"),
                NocodeValidationGroup.CREATE
        )).extracting(NocodeValidationViolation::fieldName)
                .containsExactly("name", "name");
    }

    @Test
    void validatesUpdateGroupConstraints() {
        NocodeEntityValidator validator = new NocodeEntityValidator();

        assertThat(validator.validate(
                entityDefinition,
                Map.of("name", "a", "storagePath", "/data"),
                NocodeValidationGroup.UPDATE
        )).extracting(NocodeValidationViolation::constraintType)
                .containsExactly(NocodeFieldConstraintType.SIZE);
    }

    @Test
    void emptyConstraintGroupsApplyToAllValidationGroups() {
        NocodeEntityValidator validator = new NocodeEntityValidator();

        assertThat(validator.validate(
                entityDefinition,
                Map.of("name", "demo"),
                NocodeValidationGroup.UPDATE
        )).extracting(NocodeValidationViolation::fieldName)
                .containsExactly("storagePath");
    }

    @Test
    void validateAndThrowKeepsViolations() {
        NocodeEntityValidator validator = new NocodeEntityValidator();

        assertThatThrownBy(() -> validator.validateAndThrow(
                entityDefinition,
                Map.of("name", "demo"),
                NocodeValidationGroup.CREATE
        )).isInstanceOfSatisfying(NocodeEntityValidationException.class, exception -> {
            assertThat(exception.violations()).hasSize(1);
            assertThat(exception).hasMessage("storagePath: must not be null");
        });
    }
}
