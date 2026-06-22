package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceDocsControllerTest {

    @Test
    void returnsServiceDocIndexAndMarkdownContent() {
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()));
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
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()));

        ResponseEntity<String> response = controller.openApi();

        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getBody()).contains("\"openapi\":\"3.1.0\"");
        assertThat(response.getBody()).contains("查询服务器文件列表");
    }

    @Test
    void rejectsInvalidDocIdWithBadRequest() {
        ServiceDocsController controller = new ServiceDocsController(
                new ServiceDocsScanner(new PathMatchingResourcePatternResolver()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/attachment-service/service-docs/../application.yml");
        request.setContextPath("/attachment-service");

        assertThatThrownBy(() -> controller.content(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
