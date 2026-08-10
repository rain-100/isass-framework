// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.type;

import lombok.ToString;
import vip.isass.framework.nocode.criteria.type.IPageCriteria;
import vip.isass.framework.nocode.entity.IEntity;

/**
 * 分页查询条件
 *
 * @author Rain
 */
@ToString
public class PageCriteria<E extends IEntity<E>, C extends PageCriteria<E, C>>
        implements IPageCriteria<E, C> {

    /**
     * 分页页码
     */
    private Long pageNum;

    /**
     * 每页大小
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

}

