// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.service;

import org.springframework.beans.factory.DisposableBean;
import vip.isass.framework.nocode.entity.IEntity;

import java.util.List;

/** Bridges default service methods to the Spring-owned association coordinator. */
public final class AssociationQueryCoordinatorProvider implements DisposableBean {

    private static volatile AssociationQueryCoordinator coordinator;

    public AssociationQueryCoordinatorProvider(AssociationQueryCoordinator coordinator) {
        AssociationQueryCoordinatorProvider.coordinator = coordinator;
    }

    public static <E extends IEntity<E>> List<E> populate(List<E> records, Object criteria) {
        AssociationQueryCoordinator current = coordinator;
        return current == null ? records : current.populate(records, criteria);
    }

    @Override
    public void destroy() {
        coordinator = null;
    }
}
