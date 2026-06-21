package vip.isass.framework.nocode.v3.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public record NocodeQueryCriteria(
        List<NocodeQueryCondition> conditions,
        List<NocodeQueryGroup> groups,
        List<NocodeSort> sorts,
        NocodePageRequest page,
        List<String> selectFields
) {

    public NocodeQueryCriteria {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        groups = groups == null ? List.of() : List.copyOf(groups);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
        selectFields = selectFields == null ? List.of() : List.copyOf(selectFields);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static NocodeQueryCriteria from(Map<String, ?> equalsConditions) {
        Builder builder = builder();
        if (equalsConditions != null) {
            equalsConditions.forEach(builder::where);
        }
        return builder.build();
    }

    public static final class Builder {

        private final List<NocodeQueryCondition> conditions = new ArrayList<>();
        private final List<NocodeQueryGroup> groups = new ArrayList<>();
        private final List<NocodeSort> sorts = new ArrayList<>();
        private final List<String> selectFields = new ArrayList<>();
        private NocodePageRequest page;
        private NocodeBlankStringPolicy blankStringPolicy = NocodeBlankStringPolicy.MATCH_BLANK;

        public Builder blankStringPolicy(NocodeBlankStringPolicy blankStringPolicy) {
            this.blankStringPolicy = blankStringPolicy == null
                    ? NocodeBlankStringPolicy.MATCH_BLANK
                    : blankStringPolicy;
            return this;
        }

        public Builder where(String fieldName, Object value) {
            return where(fieldName, NocodeQueryOperator.EQUALS, value);
        }

        public Builder where(String fieldName, NocodeQueryOperator operator, Object value) {
            if (shouldIgnore(value)) {
                return this;
            }
            conditions.add(new NocodeQueryCondition(fieldName, operator, value));
            return this;
        }

        public Builder group(NocodeConditionJoiner joiner, Consumer<Builder> customizer) {
            Objects.requireNonNull(customizer, "customizer");
            Builder builder = new Builder().blankStringPolicy(blankStringPolicy);
            customizer.accept(builder);
            groups.add(new NocodeQueryGroup(joiner, builder.conditions));
            return this;
        }

        public Builder sort(String fieldName, NocodeSort.Direction direction) {
            sorts.add(new NocodeSort(fieldName, direction));
            return this;
        }

        public Builder page(int pageNumber, int pageSize) {
            page = new NocodePageRequest(pageNumber, pageSize);
            return this;
        }

        public Builder select(String... fields) {
            if (fields != null) {
                for (String field : fields) {
                    selectFields.add(requireText(field, "field"));
                }
            }
            return this;
        }

        public NocodeQueryCriteria build() {
            return new NocodeQueryCriteria(conditions, groups, sorts, page, selectFields);
        }

        private boolean shouldIgnore(Object value) {
            return blankStringPolicy == NocodeBlankStringPolicy.IGNORE
                    && value instanceof String string
                    && string.isBlank();
        }

        private static String requireText(String value, String name) {
            Objects.requireNonNull(value, name);
            if (value.isBlank()) {
                throw new IllegalArgumentException(name + " must not be blank");
            }
            return value;
        }
    }
}
