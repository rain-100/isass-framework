// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.criteria.impl.field;

import vip.isass.framework.nocode.criteria.field.IIdCriteria;
import vip.isass.framework.nocode.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.nocode.entity.IIdEntity;
import java.io.Serializable;

/**
 * id 字段查询条件内置实现
 */
public class IdCriteria<PK extends Serializable, E extends IIdEntity<PK, E>, C extends IdCriteria<PK, E, C>>
    extends FullTypeCriteria<E, C>
    implements IIdCriteria<PK, E, C> {

}
