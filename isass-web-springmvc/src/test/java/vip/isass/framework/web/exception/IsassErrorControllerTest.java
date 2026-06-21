package vip.isass.framework.web.exception;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.WebRequest;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.common.web.Resp;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsassErrorControllerTest {

    @Test
    void errorJsonShouldNotThrowWhenStatusMappingsAreAbsent() {
        IsassErrorController controller = new IsassErrorController(errorAttributes(Map.of(
                "status", 404,
                "path", "/attachment-service/service-docs/missing",
                "error", "Not Found"
        )));

        Resp<?> resp = controller.errorJson(
                jsonRequest(),
                new MockHttpServletResponse()
        );

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(404);
        assertThat(resp.getMessage()).contains("GET").contains("/attachment-service/service-docs/missing");
    }

    @Test
    void errorJsonShouldMapHttpStatusWhenMappingsAreAvailable() {
        IsassErrorController controller = new IsassErrorController(
                errorAttributes(Map.of(
                        "status", 404,
                        "path", "/attachment-service/service-docs/missing",
                        "error", "Not Found"
                )),
                List.of(new WebStatusMapping())
        );

        Resp<?> resp = controller.errorJson(
                jsonRequest(),
                new MockHttpServletResponse()
        );

        assertThat(resp.getSuccess()).isFalse();
        assertThat(resp.getStatus()).isEqualTo(StatusMessageEnum.NOT_FOUND_404.getStatus());
        assertThat(resp.getMessage()).contains(StatusMessageEnum.NOT_FOUND_404.getMsg());
    }

    @Test
    void errorJsonShouldKeepOnlyHttpStatusForHtmlPageNotFound() {
        IsassErrorController controller = new IsassErrorController(errorAttributes(Map.of(
                "status", 404,
                "path", "/missing.html",
                "error", "Not Found"
        )));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.addHeader("Accept", MediaType.TEXT_HTML_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Resp<?> resp = controller.errorJson(request, response);

        assertThat(resp).isNull();
        assertThat(response.getStatus()).isEqualTo(404);
    }

    private MockHttpServletRequest jsonRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        return request;
    }

    private ErrorAttributes errorAttributes(Map<String, Object> attributes) {
        ErrorAttributes errorAttributes = mock(ErrorAttributes.class);
        when(errorAttributes.getErrorAttributes(any(WebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(attributes);
        return errorAttributes;
    }
}
