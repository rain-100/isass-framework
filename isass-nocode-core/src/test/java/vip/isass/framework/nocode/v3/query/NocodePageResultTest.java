package vip.isass.framework.nocode.v3.query;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodePageResultTest {

    @Test
    void createsImmutablePageResultAndCalculatesTotalPages() {
        NocodePageResult<String> result = new NocodePageResult<>(
                List.of("a", "b"),
                2,
                2,
                5
        );

        assertThat(result.records()).containsExactly("a", "b");
        assertThat(result.pageNumber()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.total()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        assertThatThrownBy(() -> result.records().add("c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void createsEmptyResultFromRequest() {
        NocodePageResult<Object> result = NocodePageResult.empty(new NocodePageRequest(3, 20));

        assertThat(result.records()).isEmpty();
        assertThat(result.pageNumber()).isEqualTo(3);
        assertThat(result.pageSize()).isEqualTo(20);
        assertThat(result.total()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void rejectsIllegalPageMetadata() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new NocodePageResult<>(List.of(), 0, 10, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new NocodePageResult<>(List.of(), 1, 0, 0));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> new NocodePageResult<>(List.of(), 1, 10, -1));
    }
}
