// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.security;

import vip.isass.framework.common.exception.UnifiedException;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.util.function.Supplier;

/**
 * 当前请求认证主体工具。
 */
public final class CurrentPrincipalUtil {

    private static volatile CurrentPrincipalService service;
    private static volatile Supplier<CurrentPrincipalService> serviceProvider = () -> null;

    private CurrentPrincipalUtil() {
    }

    public static void setCurrentPrincipalService(CurrentPrincipalService currentPrincipalService) {
        service = currentPrincipalService;
        serviceProvider = () -> currentPrincipalService;
    }

    public static void setCurrentPrincipalServiceProvider(Supplier<CurrentPrincipalService> currentPrincipalServiceProvider) {
        service = null;
        serviceProvider = currentPrincipalServiceProvider == null ? () -> null : currentPrincipalServiceProvider;
    }

    public static AuthenticatedPrincipal getPrincipal() {
        currentService();
        return service == null ? null : service.getPrincipal();
    }

    public static AuthenticatedPrincipal getPrincipalOrException() {
        AuthenticatedPrincipal principal = getPrincipal();
        if (principal == null) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return principal;
    }

    /** 返回当前请求的内部调用服务主体；不存在时返回 {@code null}。 */
    public static InternalServicePrincipal getInternalServicePrincipal() {
        currentService();
        return service == null ? null : service.getInternalServicePrincipal();
    }

    public static InternalServicePrincipal getInternalServicePrincipalOrException() {
        InternalServicePrincipal principal = getInternalServicePrincipal();
        if (principal == null) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return principal;
    }

    public static boolean isInternalService() {
        return getInternalServicePrincipal() != null;
    }

    public static String getInternalServiceNameOrException() {
        return getInternalServicePrincipalOrException().serviceName();
    }

    public static void checkAuthenticated() {
        getPrincipalOrException();
    }

    public static Long getTenantIdOrException() {
        Long tenantId = getPrincipalOrException().getTenantId();
        if (tenantId == null) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return tenantId;
    }

    public static Long getAppIdOrException() {
        Long appId = getPrincipalOrException().getAppId();
        if (appId == null) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return appId;
    }

    public static boolean isUser() {
        AuthenticatedPrincipal principal = getPrincipal();
        return principal != null && principal.getPrincipalType() == PrincipalType.USER;
    }

    public static boolean isApplication() {
        AuthenticatedPrincipal principal = getPrincipal();
        return principal != null && principal.getPrincipalType() == PrincipalType.APPLICATION;
    }

    public static Long getUserIdOrException() {
        AuthenticatedPrincipal principal = getPrincipalOrException();
        if (principal.getPrincipalType() != PrincipalType.USER) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return principal.getPrincipalId();
    }

    public static Long getServiceAccountIdOrException() {
        AuthenticatedPrincipal principal = getPrincipalOrException();
        if (principal.getPrincipalType() != PrincipalType.APPLICATION) {
            throw new UnifiedException(StatusMessageEnum.UN_LOGIN);
        }
        return principal.getPrincipalId();
    }

    private static void currentService() {
        if (service == null) {
            try {
                service = serviceProvider.get();
            } catch (Exception ignored) {
                // The web security module is optional for non-web applications.
            }
        }
    }
}
