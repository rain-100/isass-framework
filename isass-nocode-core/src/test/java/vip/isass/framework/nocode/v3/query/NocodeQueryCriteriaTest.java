package vip.isass.framework.nocode.v3.query;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeQueryCriteriaTest {

    @Test
    void buildsImmutableCriteriaWithMapBackedConditions() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .where("name", NocodeQueryOperator.CONTAINS, "合同")
                .where("status", "enabled")
                .where("id", NocodeQueryOperator.IN, List.of(1L, 2L))
                .sort("createdTime", NocodeSort.Direction.DESC)
                .page(2, 20)
                .select("id", "name")
                .build();

        assertThat(criteria.conditions()).extracting(NocodeQueryCondition::fieldName)
                .containsExactly("name", "status", "id");
        assertThat(criteria.conditions().get(1).operator()).isEqualTo(NocodeQueryOperator.EQUALS);
        assertThat(criteria.sorts()).containsExactly(new NocodeSort("createdTime", NocodeSort.Direction.DESC));
        assertThat(criteria.page()).isEqualTo(new NocodePageRequest(2, 20));
        assertThat(criteria.selectFields()).containsExactly("id", "name");

        assertThatThrownBy(() -> criteria.conditions().add(new NocodeQueryCondition("x", NocodeQueryOperator.EQUALS, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void canIgnoreOrKeepBlankStringConditionsByPolicy() {
        NocodeQueryCriteria ignored = NocodeQueryCriteria.builder()
                .blankStringPolicy(NocodeBlankStringPolicy.IGNORE)
                .where("name", "")
                .build();

        NocodeQueryCriteria kept = NocodeQueryCriteria.builder()
                .blankStringPolicy(NocodeBlankStringPolicy.MATCH_BLANK)
                .where("name", "")
                .build();

        assertThat(ignored.conditions()).isEmpty();
        assertThat(kept.conditions()).containsExactly(new NocodeQueryCondition("name", NocodeQueryOperator.EQUALS, ""));
    }

    @Test
    void groupsNestedConditionsWithoutDedicatedOrFields() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.builder()
                .group(NocodeConditionJoiner.OR, group -> group
                        .where("name", NocodeQueryOperator.CONTAINS, "合同")
                        .where("code", NocodeQueryOperator.CONTAINS, "HT"))
                .build();

        assertThat(criteria.groups()).hasSize(1);
        assertThat(criteria.groups().getFirst().joiner()).isEqualTo(NocodeConditionJoiner.OR);
        assertThat(criteria.groups().getFirst().conditions()).extracting(NocodeQueryCondition::fieldName)
                .containsExactly("name", "code");
    }

    @Test
    void canConvertFlatMapToEqualsConditions() {
        NocodeQueryCriteria criteria = NocodeQueryCriteria.from(Map.of(
                "name", "合同",
                "status", "enabled"
        ));

        assertThat(criteria.conditions()).containsExactlyInAnyOrder(
                new NocodeQueryCondition("name", NocodeQueryOperator.EQUALS, "合同"),
                new NocodeQueryCondition("status", NocodeQueryOperator.EQUALS, "enabled")
        );
    }
}
