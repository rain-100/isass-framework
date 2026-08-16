// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.metadata;

import org.junit.jupiter.api.Test;
import vip.isass.framework.web.security.authorization.IAuthorizationService;
import vip.isass.framework.web.security.authorization.UriRoleCodesRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultSecurityMetadataSourceProviderTest {

    @Test
    void delegatesUserAndUriRoleQueriesToAuthorizationEntrypoint() {
        IAuthorizationService authorizationService = mock(IAuthorizationService.class);
        when(authorizationService.findRoleCodesByUserId("1001")).thenReturn(List.of("ROLE_USER"));
        when(authorizationService.findRoleCodesByUri(new UriRoleCodesRequest("GET /demo")))
                .thenReturn(List.of("ROLE_DEMO"));
        DefaultSecurityMetadataSourceProvider provider =
                new DefaultSecurityMetadataSourceProvider(authorizationService);

        assertThat(provider.findRoleCodesByUserId("1001")).containsExactly("ROLE_USER");
        assertThat(provider.findRoleCodesByUri("GET /demo")).containsExactly("ROLE_DEMO");
        verify(authorizationService).findRoleCodesByUserId("1001");
        verify(authorizationService).findRoleCodesByUri(new UriRoleCodesRequest("GET /demo"));
    }
}
