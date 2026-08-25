// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.Getter;
import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.criteria.IAssociationCriteria;
import vip.isass.framework.nocode.criteria.IUpdateCriteria;
import vip.isass.framework.nocode.criteria.NullValueMode;
import vip.isass.framework.nocode.criteria.UpdateMode;
import vip.isass.framework.nocode.criteria.WhereCondition;
import vip.isass.framework.nocode.criteria.type.IOrderByCriteria;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.criteria.type.ISelectColumnCriteria;
import vip.isass.framework.nocode.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.nocode.entity.IEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聚合了 selectColumn、whereCondition、page、orderBy 查询条件
 */
public class FullTypeCriteria<E extends IEntity<E>, C extends FullTypeCriteria<E, C>>
        implements
        ISelectColumnCriteria<E, C>,
        IWhereConditionCriteria<E, C>,
        IPageCriteria<E, C>,
        IOrderByCriteria<E, C>,
        IAssociationCriteria<C>,
        IUpdateCriteria<C>,
        ICriteria<E, C> {

    private UpdateMode updateMode;

    private NullValueMode nullValueMode;

    private Collection<String> matchFields;

    private Collection<String> associationQueries;

    private Map<String, Map<String, Object>> associationCriteria;

    @Override
    public Collection<String> getAssociationQueries() {
        return associationQueries;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setAssociationQueries(Collection<String> associationQueries) {
        this.associationQueries = associationQueries;
        return (C) this;
    }

    @Override
    public Map<String, Map<String, Object>> getAssociationCriteria() {
        return associationCriteria;
    }

    @Override
    @SuppressWarnings("unchecked")
    public C setAssociationCriteria(Map<String, Map<String, Object>> associationCriteria) {
        this.associationCriteria = associationCriteria;
        return (C) this;
    }

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
        this.matchFields = matchFields == null ? null : List.copyOf(matchFields);
        return (C) this;
    }

    // region selectColumn

    private Collection<String> selectColumns;

    @Override
    public Collection<String> getSelectColumns() {
        if (selectColumns == null) {
            selectColumns = new ArrayList<>(16);
        }
        return selectColumns;
    }

    public C setSelectColumns(Collection<String> selectColumns) {
        return ISelectColumnCriteria.super.setSelectColumns(selectColumns);
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

    public C setWhereConditions(List<WhereCondition> whereConditions) {
        return IWhereConditionCriteria.super.setWhereConditions(whereConditions);
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
