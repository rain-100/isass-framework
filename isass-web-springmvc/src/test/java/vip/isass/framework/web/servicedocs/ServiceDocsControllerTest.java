// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceDocsControllerTest {

    @Test
    void returnsAssembledOpenApiJson() {
        OpenApiDocumentAssembler assembler = mock(OpenApiDocumentAssembler.class);
        when(assembler.assemble()).thenReturn("{\"openapi\":\"3.0.3\"}");

        var response = new ServiceDocsController(assembler, emptyEnhancerProvider()).openApi();

        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getBody()).isEqualTo("{\"openapi\":\"3.0.3\"}");
    }

    @Test
    void enhancesAndCachesFinalSnapshot() {
        OpenApiDocumentAssembler assembler = mock(OpenApiDocumentAssembler.class);
        when(assembler.assemble()).thenReturn("{}");
        OpenApiEnhancerSpi enhancer = mock(OpenApiEnhancerSpi.class);
        when(enhancer.enhance(anyString())).thenReturn("{\"enhanced\":true}");
        ServiceDocsController controller = new ServiceDocsController(assembler, provider(enhancer));

        assertThat(controller.openApi().getBody()).isEqualTo("{\"enhanced\":true}");
        assertThat(controller.openApi().getBody()).isEqualTo("{\"enhanced\":true}");
        verify(assembler, times(1)).assemble();
        verify(enhancer, times(1)).enhance(anyString());
    }

    @Test
    void retriesAfterEnhancementFailure() {
        OpenApiDocumentAssembler assembler = mock(OpenApiDocumentAssembler.class);
        when(assembler.assemble()).thenReturn("{}");
        OpenApiEnhancerSpi enhancer = mock(OpenApiEnhancerSpi.class);
        when(enhancer.enhance(anyString()))
                .thenThrow(new IllegalStateException("first attempt"))
                .thenReturn("{\"enhanced\":true}");
        ServiceDocsController controller = new ServiceDocsController(assembler, provider(enhancer));

        assertThatThrownBy(controller::openApi).hasMessage("first attempt");
        assertThat(controller.openApi().getBody()).isEqualTo("{\"enhanced\":true}");
        verify(enhancer, times(2)).enhance(anyString());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<OpenApiEnhancerSpi> emptyEnhancerProvider() {
        return mock(ObjectProvider.class);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<OpenApiEnhancerSpi> provider(OpenApiEnhancerSpi enhancer) {
        ObjectProvider<OpenApiEnhancerSpi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(enhancer);
        return provider;
    }
}
