// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.plus.handler;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import vip.isass.framework.common.support.SystemClock;
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.ITraceEntity;
import vip.isass.framework.nocode.entity.IVersionEntity;
import vip.isass.framework.common.security.AuthenticatedPrincipal;
import vip.isass.framework.common.security.CurrentPrincipalUtil;

/**
 * @author Rain
 */
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Object entity = metaObject.getOriginalObject();
        fillVersion(entity);

        AuthenticatedPrincipal principal = CurrentPrincipalUtil.getPrincipal();
        Long currentTenantId = principal == null ? null : principal.getTenantId();
        Long currentAppId = principal == null ? null : principal.getAppId();

        // Only tenant-scoped entities inherit the current tenant. Relationship tables can
        // also have a tenantId business key, which must retain the caller-provided value.
        fillTenant(entity, currentTenantId == null ? 0L : currentTenantId);

        // Preserve explicitly assigned business appIds, such as TenantApp during bootstrap.
        if (getFieldValByName("appId", metaObject) == null) {
            setFieldValByName("appId", currentAppId == null ? 0L : currentAppId, metaObject);
        }

        fillInsertTrace(entity, principal);
        fillDeleteFlag(entity);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        fillUpdateTrace(metaObject.getOriginalObject(), CurrentPrincipalUtil.getPrincipal());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillVersion(Object entity) {
        if (entity instanceof IVersionEntity versionEntity && versionEntity.getVersion() == null) {
            versionEntity.setVersion(IVersionEntity.DEFAULT_VERSION);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillTenant(Object entity, Long tenantId) {
        if (entity instanceof ITenantEntity tenantEntity && tenantEntity.getTenantId() == null) {
            tenantEntity.setTenantId(tenantId);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillInsertTrace(Object entity, AuthenticatedPrincipal principal) {
        if (!(entity instanceof ITraceEntity traceEntity)) return;
        Long userId = auditUserId(principal);
        String userName = auditUserName(principal);
        traceEntity.setCreateUserId(userId);
        traceEntity.setCreateUserName(userName);
        traceEntity.setModifyUserId(userId);
        traceEntity.setModifyUserName(userName);
        if (traceEntity.getCreateTime() == null) {
            traceEntity.setCreateTime(SystemClock.now());
        }
        traceEntity.setModifyTime(SystemClock.now());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillUpdateTrace(Object entity, AuthenticatedPrincipal principal) {
        if (!(entity instanceof ITraceEntity traceEntity)) return;
        traceEntity.setModifyUserId(auditUserId(principal));
        traceEntity.setModifyUserName(auditUserName(principal));
        traceEntity.setModifyTime(System.currentTimeMillis());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillDeleteFlag(Object entity) {
        if (entity instanceof ILogicDeleteEntity logicDeleteEntity) {
            logicDeleteEntity.setDeleteFlag(ILogicDeleteEntity.DEFAULT_DELETE_FLAG_VALUE);
        }
    }

    private Long auditUserId(AuthenticatedPrincipal principal) {
        return principal == null || principal.getPrincipalId() == null ? 0L : principal.getPrincipalId();
    }

    private String auditUserName(AuthenticatedPrincipal principal) {
        return principal == null
                ? StrUtil.subPre(Thread.currentThread().getName(), 32)
                : StrUtil.nullToEmpty(StrUtil.subPre(principal.getPrincipalName(), 32));
    }
}
