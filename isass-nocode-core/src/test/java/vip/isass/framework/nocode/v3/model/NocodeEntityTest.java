package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NocodeEntityTest {

    @Test
    void entityCanExposeFrameworkNeutralDefinition() {
        Attachment attachment = new Attachment();

        NocodeEntityDefinition definition = attachment.nocodeDefinition();

        assertThat(definition.entityName()).isEqualTo("attachment");
        assertThat(definition.entityType()).isEqualTo(Attachment.class);
        assertThat(definition.displayName()).isEqualTo("附件");
        assertThat(definition.idField()).map(NocodeFieldDefinition::fieldName).contains("id");
        assertThat(definition.field("name")).map(NocodeFieldDefinition::displayName).contains("附件名称");
    }

    static class Attachment implements NocodeEntity {

        @Override
        public String nocodeEntityName() {
            return "attachment";
        }

        @Override
        public String nocodeDisplayName() {
            return "附件";
        }

        @Override
        public List<NocodeFieldDefinition> nocodeFields() {
            return List.of(
                    NocodeFieldDefinition.builder("id", Long.class)
                            .displayName("主键")
                            .idField(true)
                            .build(),
                    NocodeFieldDefinition.builder("name", String.class)
                            .displayName("附件名称")
                            .queryable(true)
                            .sortable(true)
                            .build()
            );
        }
    }
}
