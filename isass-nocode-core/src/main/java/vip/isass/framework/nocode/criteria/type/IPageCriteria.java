// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.type;

import vip.isass.framework.nocode.criteria.ICriteria;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.common.page.PageConst;

/**
 * page 分页条件接口
 *
 * @author Rain
 */
public interface IPageCriteria<E extends IEntity<E>, C extends IPageCriteria<E, C>>
    extends ICriteria<E, C> {

    Long DEFAULT_PAGE_NUM = 1L;

    Long DEFAULT_PAGE_SIZE = 20L;

    Boolean DEFAULT_SEARCH_COUNT_FLAG = Boolean.TRUE;

    Long getPageNum();

    C setPageNum(Long pageNum);

    Long getPageSize();

    C setPageSize(Long pageSize);

    default C setMaxPageSize() {
        return setPageSize(PageConst.MAX_PAGE_SIZE);
    }

    Boolean getSearchCountFlag();

    C setSearchCountFlag(Boolean searchCountFlag);

}

