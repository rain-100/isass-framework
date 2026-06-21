package vip.isass.framework.nocode.v3.query;

import java.util.List;

public record NocodeQueryGroup(
        NocodeConditionJoiner joiner,
        List<NocodeQueryCondition> conditions
) {

    public NocodeQueryGroup {
        joiner = joiner == null ? NocodeConditionJoiner.AND : joiner;
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
    }
}
