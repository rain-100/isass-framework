package vip.isass.framework.common.security;

import vip.isass.framework.common.login.TerminalType;

/**
 * 当前请求中唯一的已认证主体。
 */
public interface AuthenticatedPrincipal {

    PrincipalType getPrincipalType();

    Long getPrincipalId();

    String getPrincipalName();

    Long getTenantId();

    Long getAppId();

    Long getAuthenticationExpireAt();

    TerminalType getTerminalType();

    Long getLoginLogId();

    Long getCredentialId();
}
