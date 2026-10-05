// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.advanced;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdvancedFeatureProjectorTest {

    @Test
    void translatesOnlySerializedFields() {
        AdvancedFeature feature = AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status", "hidden", "status"))
                .build();
        AdvancedFeatureProjector projector = new AdvancedFeatureProjector(
                (typeCode, optionCode) -> typeCode + ":" + optionCode);

        assertThat(projector.project(new TestEntity(), Set.of("status"), feature))
                .containsEntry("statusText", "status:1")
                .doesNotContainKey("hiddenText");
    }

    @Test
    void skipsDictionaryWithoutProvider() {
        AdvancedFeature feature = AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status"))
                .build();

        assertThat(new AdvancedFeatureProjector(null)
                .project(new TestEntity(), Set.of("status"), feature)).isEmpty();
    }

    static class TestEntity {
        private final String status = "1";
        private final String hidden = "secret";
    }
}
