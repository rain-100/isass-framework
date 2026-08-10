// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.admin;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.isass.framework.net.admin.controller.NetAdminController;
import vip.isass.framework.net.core.session.ISessionService;

@AutoConfiguration
public class NetAdminAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NetAdminController netAdminController(ISessionService sessionService) {
        return new NetAdminController(sessionService);
    }
}
