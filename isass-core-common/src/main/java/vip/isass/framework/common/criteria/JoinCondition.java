// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import lombok.Getter;

@Getter
public class JoinCondition extends BaseCondition<JoinCondition> {
    private JoinType joinType;
    private String resultProperty;

    public JoinCondition setJoinType(JoinType value) { joinType = value; return this; }
    public JoinCondition setResultProperty(String value) { resultProperty = value; return this; }
}
