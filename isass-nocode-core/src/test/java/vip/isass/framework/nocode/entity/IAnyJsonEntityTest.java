package vip.isass.framework.nocode.entity;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.DictTranslationProviderUtil;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IAnyJsonEntityTest {

    @Test
    void skipsDictTranslationWhenProviderMissing() {
        DictTranslationProviderUtil.setProvider(null);
        AdvancedFeature feature = AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status"))
                .build();

        Map<String, Object> anyJson = new TestEntity("1").advancedJson(feature);

        assertThat(anyJson).doesNotContainKey("statusText");
    }

    @Test
    void translatesDictFieldWithRegisteredProvider() {
        DictTranslationProviderUtil.setProvider((typeCode, optionCode) -> typeCode + ":" + optionCode);
        AdvancedFeature feature = AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status"))
                .build();

        Map<String, Object> anyJson = new TestEntity("1").advancedJson(feature);

        assertThat(anyJson).containsEntry("statusText", "status:1");
    }

    static class TestEntity implements IAnyJsonEntity {

        private final String status;

        TestEntity(String status) {
            this.status = status;
        }
    }
}
