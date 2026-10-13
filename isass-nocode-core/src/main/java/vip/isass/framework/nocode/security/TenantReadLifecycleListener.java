// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.security;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.entity.ITenantEntity;
import vip.isass.framework.security.CurrentPrincipalUtil;
import vip.isass.framework.security.data.TenantReadContext;
import vip.isass.framework.security.data.TenantReadPolicy;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleContext;
import vip.isass.framework.nocode.lifecycle.CrudQueryLifecycleListener;

import java.util.ArrayList;
import java.util.List;

/**
 * 为每次 NoCode 读取建立固定隔离上下文；数据库适配器对每个表实例落实条件。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class TenantReadLifecycleListener implements CrudQueryLifecycleListener {
    private static final String SCOPE = TenantReadLifecycleListener.class.getName() + ".scope";
    private final List<TenantReadPolicyProvider> providers;

    public TenantReadLifecycleListener(List<TenantReadPolicyProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public void beforeQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
        if (TenantReadContext.current() != null) {
            return;
        }
        var principal = CurrentPrincipalUtil.getPrincipal();
        if (principal == null && CurrentPrincipalUtil.isInternalService()) {
            return;
        }
        boolean platformRead = providers.stream().anyMatch(provider -> provider.allowsPlatformRead(principal));
        List<TenantReadPolicy> policies = platformRead ? List.of() : providers.stream()
                .map(provider -> provider.policy(principal))
                .toList();
        Long tenantId = principal == null ? null : principal.getTenantId();
        TenantReadPolicy policy = type -> {
            if (platformRead) {
                return List.of();
            }
            List<WhereCondition> restrictions = new ArrayList<>();
            if (ITenantEntity.class.isAssignableFrom(type)) {
                if (tenantId == null) {
                    restrictions.add(WhereCondition.and(
                            new WhereCondition("tenantId", Condition.NullCheck.IS_NULL, null),
                            new WhereCondition("tenantId", Condition.NullCheck.IS_NOT_NULL, null)));
                } else {
                    restrictions.add(WhereCondition.eq("tenantId", tenantId));
                }
            }
            policies.forEach(additional -> restrictions.addAll(additional.conditions(type)));
            return restrictions;
        };
        context.attributes().put(SCOPE, TenantReadContext.open(policy));
    }

    @Override
    public void afterQuery(CrudQueryLifecycleContext<?, ?, ?> context) {
        close(context);
    }

    @Override
    public void onFailure(CrudQueryLifecycleContext<?, ?, ?> context, Throwable error) {
        close(context);
    }

    private void close(CrudQueryLifecycleContext<?, ?, ?> context) {
        var scope = (TenantReadContext.Scope) context.attributes().remove(SCOPE);
        if (scope != null) {
            scope.close();
        }
    }
}
