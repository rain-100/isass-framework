// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import vip.isass.framework.common.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.common.security.PrincipalType;
import vip.isass.framework.entrypoint.authorization.EntrypointPermissionResolver;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DynamicPermissionAuthorizationManagerTest {
    @Test
    void requiresLocallyMappedPermissionAndDeniesUnmappedEntrypoint() {
        EntrypointPermissionResolver resolver = (method, path) ->
                "GET".equals(method) && "/asset-service/sample/task/page".equals(path)
                        ? List.of("asset.sample.task.view") : List.of();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("resolver", resolver);
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                mock(IAuthorizationService.class), beans.getBeanProvider(EntrypointPermissionResolver.class));
        PrincipalAuthenticationToken allowed = token(List.of("ROLE_LOGIN"), List.of("asset.sample.task.view"));

        assertTrue(manager.authorize(() -> allowed, request("GET", "/asset-service/sample/task/page")).isGranted());
        assertFalse(manager.authorize(() -> allowed, request("GET", "/asset-service/sample/task/delete")).isGranted());
        assertFalse(manager.authorize(() -> token(List.of("ROLE_LOGIN"), List.of()),
                request("GET", "/asset-service/sample/task/page")).isGranted());
    }

    @Test
    void superDeveloperCanDiagnoseUnmappedEntrypoints() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                mock(IAuthorizationService.class), beans.getBeanProvider(EntrypointPermissionResolver.class));
        assertTrue(manager.authorize(() -> token(List.of("ROLE_SUPER_DEV"), List.of()),
                request("POST", "/unmapped")).isGranted());
    }

    @Test
    void aggregatesPermissionMappingsFromAllEmbeddedApplications() {
        EntrypointPermissionResolver bsp = (method, path) ->
                "/bsp-service/config/parameter/page".equals(path)
                        ? List.of("bsp.config.parameter.view") : List.of();
        EntrypointPermissionResolver asset = (method, path) ->
                "/asset-service/sample/sampleTask/page".equals(path)
                        ? List.of("asset.sample.sample-task.manage") : List.of();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("bspResolver", bsp);
        beans.addBean("assetResolver", asset);
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                mock(IAuthorizationService.class), beans.getBeanProvider(EntrypointPermissionResolver.class));

        assertTrue(manager.authorize(
                () -> token(List.of("ROLE_LOGIN"), List.of("bsp.config.parameter.view")),
                request("GET", "/bsp-service/config/parameter/page")).isGranted());
        assertTrue(manager.authorize(
                () -> token(List.of("ROLE_LOGIN"), List.of("asset.sample.sample-task.manage")),
                request("GET", "/asset-service/sample/sampleTask/page")).isGranted());
    }

    private PrincipalAuthenticationToken token(List<String> roles, List<String> permissions) {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L).setTenantId(2L).setAppId(3L);
        return new PrincipalAuthenticationToken(principal, List.of(),
                new PrincipalAuthorizationContext(roles, permissions, 1L, null));
    }

    private RequestAuthorizationContext request(String method, String path) {
        return new RequestAuthorizationContext(new MockHttpServletRequest(method, path));
    }
}
