package vip.isass.framework.nocode.v3.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeCacheOperationTest {

    @Test
    void createsCacheOperationMetadata() {
        NocodeCacheOperation operation = new NocodeCacheOperation(NocodeCacheMode.CACHEABLE, "attachment", "id:1");

        assertThat(operation.mode()).isEqualTo(NocodeCacheMode.CACHEABLE);
        assertThat(operation.cacheName()).isEqualTo("attachment");
        assertThat(operation.key()).isEqualTo("id:1");
    }

    @Test
    void rejectsBlankCacheName() {
        assertThatThrownBy(() -> new NocodeCacheOperation(NocodeCacheMode.CACHEABLE, " ", "id:1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cacheName");
    }
}
