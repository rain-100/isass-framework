package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeEntityDefinitionTest {

    @Test
    void buildsEntityDefinitionWithFieldLookup() {
        NocodeEntityDefinition definition = NocodeEntityDefinition.builder("attachment", Attachment.class)
                .displayName("附件")
                .tableName("sys_attachment")
                .field(NocodeFieldDefinition.builder("id", Long.class)
                        .displayName("主键")
                        .columnName("id")
                        .idField(true)
                        .build())
                .field(NocodeFieldDefinition.builder("name", String.class)
                        .displayName("附件名称")
                        .columnName("name")
                        .queryable(true)
                        .sortable(true)
                        .build())
                .build();

        assertThat(definition.entityName()).isEqualTo("attachment");
        assertThat(definition.displayName()).isEqualTo("附件");
        assertThat(definition.field("name")).isPresent();
        assertThat(definition.field("name").orElseThrow().displayName()).isEqualTo("附件名称");
        assertThat(definition.idField()).isPresent();
        assertThat(definition.idField().orElseThrow().fieldName()).isEqualTo("id");
    }

    @Test
    void rejectsDuplicateFields() {
        NocodeFieldDefinition field = NocodeFieldDefinition.builder("id", Long.class).build();

        assertThatThrownBy(() -> NocodeEntityDefinition.builder("attachment", Attachment.class)
                .field(field)
                .field(field)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate field");
    }

    static class Attachment {
    }
}
