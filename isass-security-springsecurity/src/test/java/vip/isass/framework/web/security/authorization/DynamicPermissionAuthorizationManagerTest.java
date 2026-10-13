// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

import org.junit.jupiter.api.Test;
import vip.isass.framework.security.PrincipalAuthorizationLoader;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import vip.isass.framework.security.DefaultAuthenticatedPrincipal;
import vip.isass.framework.security.InternalServicePrincipal;
import vip.isass.framework.security.PrincipalAuthorizationContext;
import vip.isass.framework.security.PrincipalType;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.authorization.EntrypointPermissionResolver;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.entrypoint.metadata.OperationDefinition;
import vip.isass.framework.entrypoint.metadata.ServiceDefinition;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.web.security.authentication.PrincipalAuthenticationToken;
import vip.isass.framework.web.security.authorization.internal.InternalAccessRegistry;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DynamicPermissionAuthorizationManagerTest {
    @Test
    void missingFallbackAuthorizationNeverBecomesAnEmptySuccessfulResult() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                principal -> null, beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(
                new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.USER).setPrincipalId(1L));

        assertThrows(AuthenticationServiceException.class,
                () -> manager.authorize(() -> token, request("GET", "/unmapped")));
    }

    @Test
    void fallbackLoadsContextOnlyOnceAndUpdatesStandardAuthorities() {
        PrincipalAuthorizationLoader service = mock(PrincipalAuthorizationLoader.class);
        PrincipalAuthorizationContext context = PrincipalAuthorizationContext.roles(
                List.of("ROLE_SUPER_DEV"), 1L, null);
        when(service.loadRoles(any())).thenReturn(context);
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                service, beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));
        PrincipalAuthenticationToken token = new PrincipalAuthenticationToken(
                new DefaultAuthenticatedPrincipal().setPrincipalType(PrincipalType.USER).setPrincipalId(1L));

        assertTrue(manager.authorize(() -> token, request("GET", "/unmapped")).isGranted());
        assertTrue(manager.authorize(() -> token, request("GET", "/unmapped")).isGranted());
        assertSame(context, token.getAuthorizationContext());
        assertEquals("ROLE_SUPER_DEV", token.getAuthorities().iterator().next().getAuthority());
        verify(service).loadRoles(any());
        verify(service, never()).load(any());
    }

    @Test
    void ordinaryUserUpgradesRoleOnlyContextAndReusesCompleteAuthorization() {
        PrincipalAuthorizationLoader loader = mock(PrincipalAuthorizationLoader.class);
        when(loader.loadRoles(any())).thenReturn(PrincipalAuthorizationContext.roles(List.of("ROLE_LOGIN"), 1L, null));
        when(loader.load(any())).thenReturn(new PrincipalAuthorizationContext(
                List.of("ROLE_LOGIN"), List.of("bsp.auth.user.view"), 2L, null));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("resolver", (EntrypointPermissionResolver) (method, path) -> List.of("bsp.auth.user.view"));
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                loader, beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));
        PrincipalAuthenticationToken token = token(List.of("ROLE_LOGIN"), List.of());
        token.setAuthorizationContext(null);
        assertTrue(manager.authorize(() -> token, request("GET", "/users")).isGranted());
        assertTrue(manager.authorize(() -> token, request("GET", "/users")).isGranted());
        verify(loader).loadRoles(any());
        verify(loader).load(any());
    }

    @Test
    void requiresLocallyMappedPermissionAndDeniesUnmappedEntrypoint() {
        EntrypointPermissionResolver resolver = (method, path) ->
                "GET".equals(method) && "/asset-service/sample/task/page".equals(path)
                        ? List.of("asset.sample.task.view") : List.of();
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("resolver", resolver);
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                mock(PrincipalAuthorizationLoader.class), beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));
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
                mock(PrincipalAuthorizationLoader.class), beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));
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
                mock(PrincipalAuthorizationLoader.class), beans.getBeanProvider(EntrypointPermissionResolver.class),
                new InternalAccessRegistry(List.of(), emptyDefinitions()));

        assertTrue(manager.authorize(
                () -> token(List.of("ROLE_LOGIN"), List.of("bsp.config.parameter.view")),
                request("GET", "/bsp-service/config/parameter/page")).isGranted());
        assertTrue(manager.authorize(
                () -> token(List.of("ROLE_LOGIN"), List.of("asset.sample.sample-task.manage")),
                request("GET", "/asset-service/sample/sampleTask/page")).isGranted());
    }

    @Test
    void allowedInternalOperationWinsWhenBusinessPrincipalLacksPermission() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        InternalAccessRegistry internalAccess = new InternalAccessRegistry(List.of(
                builder -> builder.allow(InternalTestService.class, InternalTestService::refresh)),
                internalTestDefinitions());
        DynamicPermissionAuthorizationManager manager = new DynamicPermissionAuthorizationManager(
                mock(PrincipalAuthorizationLoader.class), beans.getBeanProvider(EntrypointPermissionResolver.class),
                internalAccess);
        PrincipalAuthenticationToken businessAndInternal = new PrincipalAuthenticationToken(
                token(List.of("ROLE_LOGIN"), List.of()).getPrincipal(),
                new InternalServicePrincipal("asset-service", "internal-key", "request-1"),
                new PrincipalAuthorizationContext(List.of("ROLE_LOGIN"), List.of(), 1L, null));

        assertTrue(manager.authorize(() -> businessAndInternal,
                request("POST", "/bsp-service/config/parameter/refresh")).isGranted());
        assertFalse(manager.authorize(() -> businessAndInternal,
                request("POST", "/bsp-service/config/parameter/unlisted")).isGranted());
    }

    private PrincipalAuthenticationToken token(List<String> roles, List<String> permissions) {
        DefaultAuthenticatedPrincipal principal = new DefaultAuthenticatedPrincipal()
                .setPrincipalType(PrincipalType.USER).setPrincipalId(1L).setTenantId(2L).setAppId(3L);
        return new PrincipalAuthenticationToken(principal,
                new PrincipalAuthorizationContext(roles, permissions, 1L, null));
    }

    private RequestAuthorizationContext request(String method, String path) {
        return new RequestAuthorizationContext(new MockHttpServletRequest(method, path));
    }

    private ServiceDefinitionRegistry emptyDefinitions() {
        return new ServiceDefinitionRegistry() {
            @Override
            public List<ServiceDefinition> all() {
                return List.of();
            }

            @Override
            public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
                return Optional.empty();
            }
        };
    }

    private ServiceDefinitionRegistry internalTestDefinitions() {
        try {
            EntrypointInfo info = InternalTestService.class.getAnnotation(EntrypointInfo.class);
            var method = InternalTestService.class.getMethod("refresh");
            EntrypointOperation operation = method.getAnnotation(EntrypointOperation.class);
            OperationDefinition operationDefinition = new OperationDefinition(
                    operation.operationName(), operation.displayName(), operation.description(),
                    operation.displayOrder(), operation.httpMethod(), operation.accessStrategy(), method,
                    List.of(), method.getGenericReturnType(), false);
            ServiceDefinition definition = new ServiceDefinition(
                    info.serviceName(), info.contextName(), info.resourceName(), InternalTestService.class,
                    List.of(operationDefinition), true);
            return new ServiceDefinitionRegistry() {
                @Override
                public List<ServiceDefinition> all() {
                    return List.of(definition);
                }

                @Override
                public Optional<ServiceDefinition> find(String serviceName, String contextName, String resourceName) {
                    return definition.serviceName().equals(serviceName)
                            && definition.contextName().equals(contextName)
                            && definition.resourceName().equals(resourceName)
                            ? Optional.of(definition) : Optional.empty();
                }
            };
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @EntrypointInfo(serviceName = "bsp-service", contextName = "config", resourceName = "parameter")
    private interface InternalTestService extends IEntrypoint {
        @EntrypointOperation(operationName = "refresh", displayName = "刷新", httpMethod = HttpMethod.POST)
        void refresh();
    }
}
