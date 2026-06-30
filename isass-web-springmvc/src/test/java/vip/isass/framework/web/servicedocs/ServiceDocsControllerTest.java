package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServiceDocsControllerTest {

    @Test
    void returnsGeneratedOpenApiJsonFromOpenapi3Directory() {
        ServiceDocsController controller = new ServiceDocsController(
                new DefaultResourceLoader(),
                emptyEnhancerProvider());

        ResponseEntity<String> response = controller.openApi();

        assertThat(response.getHeaders().getContentType().toString())
                .isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getBody()).contains("\"openapi\":\"3.1.0\"");
        assertThat(response.getBody()).contains("查询服务器文件列表");
    }

    @Test
    void returnsEnhancedOpenApiJsonAndCachesFinalResult() {
        OpenApiEnhancerSpi enhancer = mock(OpenApiEnhancerSpi.class);
        when(enhancer.enhance(anyString())).thenReturn(new String("{\"enhanced\":true}"));
        ObjectProvider<OpenApiEnhancerSpi> provider = provider(enhancer);
        ServiceDocsController controller = new ServiceDocsController(
                new DefaultResourceLoader(),
                provider);

        ResponseEntity<String> first = controller.openApi();
        ResponseEntity<String> second = controller.openApi();

        assertThat(first.getBody()).isEqualTo("{\"enhanced\":true}");
        assertThat(second.getBody()).isSameAs(first.getBody());
        verify(enhancer, times(1)).enhance(anyString());
    }

    @Test
    void retriesAfterEnhancementFailure() {
        OpenApiEnhancerSpi enhancer = mock(OpenApiEnhancerSpi.class);
        when(enhancer.enhance(anyString()))
                .thenThrow(new IllegalStateException("first attempt"))
                .thenReturn("{\"enhanced\":true}");
        ServiceDocsController controller = new ServiceDocsController(
                new DefaultResourceLoader(),
                provider(enhancer));

        assertThatThrownBy(controller::openApi)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("first attempt");
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
