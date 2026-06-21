package vip.isass.framework.web.nocode;

import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import vip.isass.framework.nocode.v3.query.NocodeBlankStringPolicy;
import vip.isass.framework.nocode.v3.query.NocodePageRequest;
import vip.isass.framework.nocode.v3.query.NocodeQueryCondition;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;
import vip.isass.framework.nocode.v3.query.NocodeQueryOperator;
import vip.isass.framework.nocode.v3.query.NocodeSort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeSpringMvcQueryCriteriaParserTest {

    @Test
    void parsesEqualsConditionsFromQueryParameters() {
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("name", "demo");
        parameters.add("status", "enabled");
        parameters.add("tag", "a");
        parameters.add("tag", "b");

        NocodeQueryCriteria criteria = new NocodeSpringMvcQueryCriteriaParser().parse(parameters);

        assertThat(criteria.conditions()).containsExactly(
                new NocodeQueryCondition("name", NocodeQueryOperator.EQUALS, "demo"),
                new NocodeQueryCondition("status", NocodeQueryOperator.EQUALS, "enabled"),
                new NocodeQueryCondition("tag", NocodeQueryOperator.EQUALS, List.of("a", "b"))
        );
    }

    @Test
    void parsesPageSortAndSelectReservedParameters() {
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("page", "2");
        parameters.add("size", "50");
        parameters.add("sort", "createdTime,desc");
        parameters.add("sort", "-id");
        parameters.add("select", "id,name");
        parameters.add("select", "status");

        NocodeQueryCriteria criteria = new NocodeSpringMvcQueryCriteriaParser().parse(parameters);

        assertThat(criteria.page()).isEqualTo(new NocodePageRequest(2, 50));
        assertThat(criteria.sorts()).containsExactly(
                new NocodeSort("createdTime", NocodeSort.Direction.DESC),
                new NocodeSort("id", NocodeSort.Direction.DESC)
        );
        assertThat(criteria.selectFields()).containsExactly("id", "name", "status");
        assertThat(criteria.conditions()).isEmpty();
    }

    @Test
    void appliesBlankStringPolicy() {
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("name", "");

        NocodeQueryCriteria criteria = new NocodeSpringMvcQueryCriteriaParser(NocodeBlankStringPolicy.IGNORE)
                .parse(parameters);

        assertThat(criteria.conditions()).isEmpty();
    }

    @Test
    void rejectsInvalidPageNumber() {
        LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("pageNumber", "0");

        assertThatThrownBy(() -> new NocodeSpringMvcQueryCriteriaParser().parse(parameters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pageNumber must be a positive integer");
    }
}
