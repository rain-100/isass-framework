package vip.isass.framework.web.servicedocs;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author Rain
 */
@RestController
public class ServiceDocsController {

    private static final MediaType TEXT_MARKDOWN = new MediaType("text", "markdown", StandardCharsets.UTF_8);

    private final ServiceDocsScanner serviceDocsScanner;

    public ServiceDocsController(ServiceDocsScanner serviceDocsScanner) {
        this.serviceDocsScanner = serviceDocsScanner;
    }

    @GetMapping({"/service-docs", "/${spring.application.name}/service-docs"})
    public List<ServiceDoc> list() {
        return serviceDocsScanner.findAll();
    }

    @GetMapping({"/service-docs/**", "/${spring.application.name}/service-docs/**"})
    public ResponseEntity<String> content(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String prefix = resolveServiceDocsPrefix(requestUri);
        String docId = requestUri.substring(prefix.length());
        try {
            return ResponseEntity.ok().contentType(TEXT_MARKDOWN).body(serviceDocsScanner.readContent(docId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    private String resolveServiceDocsPrefix(String requestUri) {
        int index = requestUri.indexOf("/service-docs/");
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid service docs path");
        }
        return requestUri.substring(0, index) + "/service-docs/";
    }
}
