package vip.isass.framework.web.servicedocs;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
}
