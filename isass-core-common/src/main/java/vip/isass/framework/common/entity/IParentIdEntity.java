// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.entity;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;
import java.util.List;

/**
 * @author Rain
 */
public interface IParentIdEntity<PK extends Serializable, E extends IParentIdEntity<PK, E>>
    extends IPkEntity<PK, E> {

    /**
     * @return 父 id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    PK getParentId();

    /**
     * 设置父 id
     *
     * @param parentId parent id
     */
    void setParentId(PK parentId);

    /** Non-persistent parent projection. */
    E getParent();

    void setParent(E parent);

    /** Non-persistent direct-child projection. */
    List<E> getChildren();

    void setChildren(List<E> children);

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    default List<EntityAssociation> associations() {
        Class<? extends IEntity<?>> type = (Class) getClass();
        return List.of(
                EntityAssociation.one("parent", type, "parentId", "id", false),
                EntityAssociation.many("children", type, "id", "parentId", false));
    }

    /**
     * 标记为顶级实体
     *
     * @return this object
     */
    @SuppressWarnings("unchecked")
    default E markAsTopEntity() {
        setParentId(null);
        return (E) this;
    }

    @SuppressWarnings("unchecked")
    default E randomParentId() {
        setParentId(randomPk());
        return (E) this;
    }

    @Override
    default E randomEntity() {
        return randomParentId();
    }

}
