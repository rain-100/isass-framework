package vip.isass.framework.nocode.v2.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.DictTranslationProviderUtil;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IAnyJsonEntityTest {

    @AfterEach
    void tearDown() {
        IAnyJsonEntity.ADVANCED_FEATURE.remove();
        DictTranslationProviderUtil.setProvider(null);
    }

    @Test
    void skipsDictTranslationWhenProviderMissing() {
        DictTranslationProviderUtil.setProvider(null);
        IAnyJsonEntity.ADVANCED_FEATURE.set(AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status"))
                .build());

        Map<String, Object> anyJson = new TestEntity("1").anyJson();

        assertThat(anyJson).doesNotContainKey("statusText");
    }

    @Test
    void translatesDictFieldWithRegisteredProvider() {
        DictTranslationProviderUtil.setProvider((typeCode, optionCode) -> typeCode + ":" + optionCode);
        IAnyJsonEntity.ADVANCED_FEATURE.set(AdvancedFeature.builder()
                .dictTranslation(Map.of("status", "status"))
                .build());

        Map<String, Object> anyJson = new TestEntity("1").anyJson();

        assertThat(anyJson).containsEntry("statusText", "status:1");
    }

    static class TestEntity implements IAnyJsonEntity {

        private final String status;

        TestEntity(String status) {
            this.status = status;
        }
    }
}
