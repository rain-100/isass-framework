// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.page;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    void createsImmutableRecordsAndCalculatesPageCount() {
        Page<String> result = Page.of(List.of("a", "b"), 2, 2, 5);

        assertThat(result.getRecords()).containsExactly("a", "b");
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(5);
        assertThat(result.getPageCount()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
        assertThatThrownBy(() -> result.getRecords().add("c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exposesJavaBeanPropertiesForOpenApiIntrospection() throws Exception {
        var propertyNames = List.of(Introspector.getBeanInfo(Page.class, Object.class)
                        .getPropertyDescriptors()).stream()
                .map(java.beans.PropertyDescriptor::getName)
                .toList();

        assertThat(propertyNames).containsExactlyInAnyOrder(
                "records", "pageNum", "pageSize", "total", "pageCount");
    }

    @Test
    void rejectsIllegalPageMetadata() {
        assertThatThrownBy(() -> Page.empty(0, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Page.empty(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Page.of(List.of(), 1, 20, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void calculatesPageCountWithoutOverflow() {
        Page<Object> result = Page.of(List.of(), 1, 2, Long.MAX_VALUE);

        assertThat(result.getPageCount()).isEqualTo(Long.MAX_VALUE / 2 + 1);
    }
}
