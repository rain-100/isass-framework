package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.validation.NocodeFieldConstraint;
import vip.isass.framework.nocode.v3.validation.NocodeValidationGroup;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeFieldDefinitionTest {

    @Test
    void defaultsFieldWriteAndAutoFillMetadata() {
        NocodeFieldDefinition field = NocodeFieldDefinition.builder("name", String.class).build();

        assertThat(field.clientWritable()).isTrue();
        assertThat(field.autoFill()).isEqualTo(NocodeFieldAutoFill.NONE);
        assertThat(field.autoFill().fillOnCreate()).isFalse();
        assertThat(field.autoFill().fillOnUpdate()).isFalse();
        assertThat(field.constraints()).isEmpty();
    }

    @Test
    void canConfigureClientWritableAndTimestampAutoFill() {
        NocodeFieldDefinition field = NocodeFieldDefinition.builder("createdTime", Long.class)
                .clientWritable(false)
                .autoFill(NocodeFieldAutoFill.CREATE_TIME)
                .build();

        assertThat(field.clientWritable()).isFalse();
        assertThat(field.autoFill()).isEqualTo(NocodeFieldAutoFill.CREATE_TIME);
        assertThat(field.autoFill().fillOnCreate()).isTrue();
        assertThat(field.autoFill().fillOnUpdate()).isFalse();
    }

    @Test
    void canConfigureValidationConstraints() {
        NocodeFieldDefinition field = NocodeFieldDefinition.builder("name", String.class)
                .constraint(NocodeFieldConstraint.notBlank(NocodeValidationGroup.CREATE))
                .build();

        assertThat(field.constraints()).hasSize(1);
        assertThat(field.constraints().getFirst().appliesTo(NocodeValidationGroup.CREATE)).isTrue();
        assertThat(field.constraints().getFirst().appliesTo(NocodeValidationGroup.UPDATE)).isFalse();
    }
}
