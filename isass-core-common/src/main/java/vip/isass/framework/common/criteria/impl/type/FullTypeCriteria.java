// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria.impl.type;

import lombok.Getter;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.criteria.IRelatedQueryCriteria;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.RelatedCondition;
import vip.isass.framework.common.criteria.IUpdateCriteria;
import vip.isass.framework.common.criteria.NullValueMode;
import vip.isass.framework.common.criteria.UpdateMode;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.type.IOrderByCriteria;
import vip.isass.framework.common.criteria.type.IPageCriteria;
import vip.isass.framework.common.criteria.type.IReturnFieldCriteria;
import vip.isass.framework.common.entity.IEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 聚合了 returnField、whereCondition、page、orderBy 查询条件
 */
public class FullTypeCriteria<E extends IEntity<E>, C extends FullTypeCriteria<E, C>>
        implements
        IReturnFieldCriteria<E, C>,
        IRelatedQueryCriteria<E, C>,
        IPageCriteria<E, C>,
        IOrderByCriteria<E, C>,
        IUpdateCriteria<C>,
        ICriteria<E, C> {

    private List<RelatedCondition> loadRelated;
    private List<JoinCondition> joinConditions;
    @Getter
    private ICriteria<?, ?> fromCriteria;

    @Override
    public List<RelatedCondition> getLoadRelated() {
        if (loadRelated == null) loadRelated = new ArrayList<>();
        return loadRelated;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setLoadRelated(List<RelatedCondition> conditions) {
        loadRelated = conditions;
        return (C) this;
    }

    @Override
    public List<JoinCondition> getJoinConditions() {
        if (joinConditions == null) joinConditions = new ArrayList<>();
        return joinConditions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setJoinConditions(List<JoinCondition> conditions) {
        joinConditions = conditions;
        return (C) this;
    }

    @SuppressWarnings("unchecked")
    public C setFromCriteria(ICriteria<?, ?> criteria) {
        fromCriteria = criteria;
        return (C) this;
    }

    private UpdateMode updateMode;

    private NullValueMode nullValueMode;

    private Collection<String> matchFields;

    @Override
    public UpdateMode getUpdateMode() {
        return updateMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
        return (C) this;
    }

    @Override
    public NullValueMode getNullValueMode() {
        return nullValueMode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setNullValueMode(NullValueMode nullValueMode) {
        this.nullValueMode = nullValueMode;
        return (C) this;
    }

    @Override
    public Collection<String> getMatchFields() {
        return matchFields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setMatchFields(Collection<String> matchFields) {
        this.matchFields = matchFields;
        return (C) this;
    }

    // region returnField

    private Collection<String> returnFields;

    @Override
    public Collection<String> getReturnFields() {
        if (returnFields == null) {
            returnFields = new ArrayList<>(16);
        }
        return returnFields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setReturnFields(Collection<String> returnFields) {
        this.returnFields = returnFields;
        return (C) this;
    }

    // endregion

    // region whereCondition

    private List<WhereCondition> whereConditions;

    public List<WhereCondition> getWhereConditions() {
        if (whereConditions == null) {
            whereConditions = new ArrayList<>();
        }
        return whereConditions;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setWhereConditions(List<WhereCondition> whereConditions) {
        this.whereConditions = whereConditions;
        return (C) this;
    }

    // endregion

    // region page

    /**
     * 分页页码，从1开始，默认1
     */
    private Long pageNum;

    /**
     * 每页大小，默认20
     */
    private Long pageSize;

    private Boolean searchCountFlag;

    @Override
    public Long getPageNum() {
        return pageNum == null ? DEFAULT_PAGE_NUM : pageNum < 1L ? 1L : pageNum;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setPageNum(Long pageNum) {
        this.pageNum = pageNum;
        return (C) this;
    }

    @Override
    public Long getPageSize() {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize < 1L ? DEFAULT_PAGE_SIZE : pageSize;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setPageSize(Long pageSize) {
        this.pageSize = pageSize;
        return (C) this;
    }

    @Override
    public Boolean getSearchCountFlag() {
        return searchCountFlag == null ? DEFAULT_SEARCH_COUNT_FLAG : searchCountFlag;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setSearchCountFlag(Boolean searchCountFlag) {
        this.searchCountFlag = searchCountFlag;
        return (C) this;
    }

    // endregion

    // region orderBy

    @Getter
    private String orderBy;

    @Override
    @SuppressWarnings("unchecked")
    public C setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return (C) this;
    }

    // endregion

}
