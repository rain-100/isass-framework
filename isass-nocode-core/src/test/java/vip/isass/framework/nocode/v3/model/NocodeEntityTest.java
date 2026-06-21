package vip.isass.framework.nocode.v3.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Test
    void entityCanFormatAndSetupLongTimestampForDebugging() {
        Attachment attachment = new Attachment();
        long timestamp = LocalDateTime.of(2022, 1, 1, 12, 0, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        attachment.setCreateTime(timestamp);

        assertThat(attachment.formatTimestamp(Attachment::getCreateTime))
                .isEqualTo("2022-01-01 12:00:00");

        attachment.setupTimestamp("2023-02-03 04:05:06", Attachment::setCreateTime);

        assertThat(attachment.getCreateTime()).isEqualTo(
                LocalDateTime.of(2023, 2, 3, 4, 5, 6)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
        );
    }

    static class Attachment implements NocodeEntity {

        private Long createTime;

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

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }
    }
}
