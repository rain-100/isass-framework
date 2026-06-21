package vip.isass.framework.nocode.v3.query;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.v3.model.NocodeEntityDefinition;
import vip.isass.framework.nocode.v3.model.NocodeFieldDefinition;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeQueryValidatorTest {

    private final NocodeEntityDefinition definition = NocodeEntityDefinition.builder("attachment", Object.class)
            .field(NocodeFieldDefinition.builder("id", Long.class)
                    .idField(true)
                    .queryable(true)
                    .sortable(true)
                    .build())
            .field(NocodeFieldDefinition.builder("name", String.class)
                    .queryable(true)
                    .sortable(true)
                    .build())
            .field(NocodeFieldDefinition.builder("secret", String.class)
                    .queryable(false)
                    .sortable(false)
                    .build())
            .build();

    @Test
    void allowsKnownQueryableAndSortableFields() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .select("id", "name")
                .where("name", NocodeQueryOperator.CONTAINS, "合同")
                .group(NocodeConditionJoiner.OR, group -> group
                        .where("id", 1L)
                        .where("name", NocodeQueryOperator.STARTS_WITH, "A"))
                .sort("id", NocodeSort.Direction.DESC)
                .build();

        assertThatCode(() -> NocodeQueryValidator.validate(definition, criteria))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownFields() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .where("missing", 1)
                .build();

        assertThatThrownBy(() -> NocodeQueryValidator.validate(definition, criteria))
                .isInstanceOf(NocodeQueryValidationException.class)
                .hasMessageContaining("Unknown field")
                .hasMessageContaining("missing");
    }

    @Test
    void rejectsNonQueryableFields() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .where("secret", "value")
                .build();

        assertThatThrownBy(() -> NocodeQueryValidator.validate(definition, criteria))
                .isInstanceOf(NocodeQueryValidationException.class)
                .hasMessageContaining("not queryable")
                .hasMessageContaining("secret");
    }

    @Test
    void rejectsNonSortableFields() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .sort("secret", NocodeSort.Direction.ASC)
                .build();

        assertThatThrownBy(() -> NocodeQueryValidator.validate(definition, criteria))
                .isInstanceOf(NocodeQueryValidationException.class)
                .hasMessageContaining("not sortable")
                .hasMessageContaining("secret");
    }
}
