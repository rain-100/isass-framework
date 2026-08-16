// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import org.junit.jupiter.api.Test;
import vip.isass.framework.entrypoint.IEntrypoint;
import vip.isass.framework.entrypoint.annotation.EntrypointOperation;
import vip.isass.framework.entrypoint.metadata.HttpMethod;
import vip.isass.framework.nocode.service.ICrudService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoConfigurationTest {

    @Test
    void classifierMarksOnlyStandardCrudOperationsAsNocode() {
        var classifier = new AutoConfiguration().nocodeEntrypointClassifier();
        var page = java.util.Arrays.stream(ICrudService.class.getMethods())
                .filter(method -> method.getName().equals("page"))
                .findFirst().orElseThrow();
        var custom = java.util.Arrays.stream(CustomEntrypoint.class.getMethods())
                .filter(method -> method.getName().equals("publish"))
                .findFirst().orElseThrow();

        assertTrue(classifier.isNocode(ICrudService.class, page));
        assertFalse(classifier.isNocode(ICrudService.class, custom));
    }

    private interface CustomEntrypoint extends IEntrypoint {
        @EntrypointOperation(operationName = "publish", displayName = "发布", httpMethod = HttpMethod.POST)
        void publish();
    }
}
