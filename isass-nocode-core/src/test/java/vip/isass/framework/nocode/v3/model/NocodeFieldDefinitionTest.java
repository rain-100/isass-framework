package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeFieldDefinitionTest {

    @Test
    void defaultsFieldWriteAndAutoFillMetadata() {
        NocodeFieldDefinition field = NocodeFieldDefinition.builder("name", String.class).build();

        assertThat(field.clientWritable()).isTrue();
        assertThat(field.autoFill()).isEqualTo(NocodeFieldAutoFill.NONE);
        assertThat(field.autoFill().fillOnCreate()).isFalse();
        assertThat(field.autoFill().fillOnUpdate()).isFalse();
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
}
