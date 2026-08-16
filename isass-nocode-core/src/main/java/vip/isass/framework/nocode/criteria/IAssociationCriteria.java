// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria;

import java.util.Collection;
import java.util.Map;

/** Optional, explicit one-level association expansion requested by a caller. */
public interface IAssociationCriteria<C extends IAssociationCriteria<C>> {

    Collection<String> getAssociationQueries();

    C setAssociationQueries(Collection<String> associations);

    Map<String, Map<String, Object>> getAssociationCriteria();

    C setAssociationCriteria(Map<String, Map<String, Object>> criteria);
}
