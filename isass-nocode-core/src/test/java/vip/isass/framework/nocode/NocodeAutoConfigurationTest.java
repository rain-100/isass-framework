// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.nocode.service.ICrudService;
import vip.isass.framework.nocode.service.ITreeQueryService;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class NocodeAutoConfigurationTest {

    @Test
    void authorizationPreservesNullableCursorArgumentsAndStillEvaluatesPermissions() {
        var operation = new vip.isass.framework.entrypoint.metadata.OperationDefinition(
                "cursorPage", "游标分页", "", 0, HttpMethod.GET, null, null,
                java.util.List.of(), Object.class, true);
        var service = new vip.isass.framework.entrypoint.metadata.ServiceDefinition(
                "test-service", "sample", "sampleGroup", CustomEntrypoint.class,
                java.util.List.of(operation), true);
        Object criteria = new Object();
        Object[] arguments = {criteria, null, null};
        var captured = new java.util.concurrent.atomic.AtomicReference<vip.isass.framework.nocode.security.NocodeAuthorizationContext>();
        var configuration = new NocodeAutoConfiguration();
        configuration.nocodeInvocationAuthorizer(captured::set).check(service, operation, arguments);
        org.junit.jupiter.api.Assertions.assertEquals(Arrays.asList(criteria, null, null), captured.get().arguments());
        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
                () -> configuration.nocodeInvocationAuthorizer(context -> {
                    throw new SecurityException("denied");
                }).check(service, operation, arguments));
    }

    @Test
    void classifierMarksStandardCrudAndTreeCapabilityOperationsAsNocode() {
        var classifier = new NocodeAutoConfiguration().nocodeEntrypointClassifier();
        var page = Arrays.stream(ICrudService.class.getMethods())
                .filter(method -> method.getName().equals("page"))
                .findFirst()
                .orElseThrow();
        var custom = Arrays.stream(CustomEntrypoint.class.getMethods())
                .filter(method -> method.getName().equals("publish"))
                .findFirst()
                .orElseThrow();
        var tree = Arrays.stream(ITreeQueryService.class.getMethods())
                .filter(method -> method.getName().equals("tree"))
                .findFirst()
                .orElseThrow();
        var descendantIds = Arrays.stream(ITreeQueryService.class.getMethods())
                .filter(method -> method.getName().equals("descendantIds"))
                .findFirst()
                .orElseThrow();

        assertTrue(classifier.isNocode(ICrudService.class, page));
        assertTrue(classifier.isNocode(ITreeQueryService.class, tree));
        assertTrue(classifier.isNocode(ITreeQueryService.class, descendantIds));
        assertFalse(classifier.isNocode(ICrudService.class, custom));
    }

    private interface CustomEntrypoint extends IEntrypoint {
        @EntrypointOperation(operationName = "publish", displayName = "发布", httpMethod = HttpMethod.POST)
        void publish();
    }
}
