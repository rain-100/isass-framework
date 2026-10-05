// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/** 查询后的批量关联装配条件；关联键依据实体生成元数据解析。 */
@Getter
public class RelatedCondition {
    private String property;
    private String localKey;
    private String targetKey;
    private ICriteria<?, ?> criteria;

    public RelatedCondition setProperty(String value) { property = value; return this; }
    public RelatedCondition setLocalKey(String value) { localKey = value; return this; }
    public RelatedCondition setTargetKey(String value) { targetKey = value; return this; }
    public RelatedCondition setCriteria(ICriteria<?, ?> value) { criteria = value; return this; }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new IllegalArgumentException("未知 loadRelated 字段: " + property);
    }
}
