package vip.isass.framework.common.security;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import vip.isass.framework.common.login.TerminalType;

/**
 * {@link AuthenticatedPrincipal} 的默认实现。
 */
@Getter
@Setter
@Accessors(chain = true)
public class DefaultAuthenticatedPrincipal implements AuthenticatedPrincipal {

    private PrincipalType principalType;

    private Long principalId;

    private String principalName;

    private Long tenantId;

    private Long appId;

    private Long authenticationExpireAt;

    private TerminalType terminalType;

    private Long loginLogId;

    private Long credentialId;
}
