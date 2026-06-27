package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceDocsControllerTest {

    @Test
    void returnsServiceDocIndexAndMarkdownContent() {
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()),
                emptyEnhancerProvider());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/attachment-service/service-docs/database/attachment-db");
        request.setContextPath("/attachment-service");

        List<ServiceDoc> docs = controller.list();
        ResponseEntity<String> response = controller.content(request);

        assertThat(docs).extracting(ServiceDoc::id).contains("database/attachment-db");
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/markdown;charset=UTF-8");
        assertThat(response.getBody()).contains("# Attachment Database");
    }

    @Test
    void returnsGeneratedOpenApiJsonFromServiceDocsApiDirectory() {
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()),
                emptyEnhancerProvider());

        ResponseEntity<String> response = controller.openApi();

        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getBody()).contains("\"openapi\":\"3.1.0\"");
        assertThat(response.getBody()).contains("查询服务器文件列表");
    }

    @Test
    void returnsEnhancedOpenApiJsonWhenEnhancerIsAvailable() {
        OpenApiEnhancerSpi enhancer = () -> "{\"enhanced\":true}";
        ObjectProvider<OpenApiEnhancerSpi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(enhancer);
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()),
                provider);

        ResponseEntity<String> response = controller.openApi();

        assertThat(response.getBody()).isEqualTo("{\"enhanced\":true}");
    }

    @Test
    void rejectsInvalidDocIdWithBadRequest() {
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()),
                emptyEnhancerProvider());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/attachment-service/service-docs/../application.yml");
        request.setContextPath("/attachment-service");

        assertThatThrownBy(() -> controller.content(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<OpenApiEnhancerSpi> emptyEnhancerProvider() {
        return mock(ObjectProvider.class);
    }
}
