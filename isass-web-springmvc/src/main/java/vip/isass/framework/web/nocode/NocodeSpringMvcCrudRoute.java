package vip.isass.framework.web.nocode;

import org.springframework.web.bind.annotation.RequestMethod;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;

import java.util.List;
import java.util.Objects;

/**
 * Spring MVC route descriptor for future nocode v3 dynamic CRUD endpoints.
 */
public record NocodeSpringMvcCrudRoute(
        NocodeCrudOperation operation,
        RequestMethod method,
        String pathPattern,
        List<String> pathVariables
) {

    public static final String DEFAULT_BASE_PATH = "/nocode";

    public NocodeSpringMvcCrudRoute {
        operation = Objects.requireNonNull(operation, "operation");
        method = Objects.requireNonNull(method, "method");
        pathPattern = requireText(pathPattern, "pathPattern");
        pathVariables = pathVariables == null ? List.of() : List.copyOf(pathVariables);
    }

    public static List<NocodeSpringMvcCrudRoute> defaultRoutes() {
        return routes(DEFAULT_BASE_PATH);
    }

    public static List<NocodeSpringMvcCrudRoute> routes(String basePath) {
        String base = normalizeBasePath(basePath);
        return List.of(
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.LIST,
                        RequestMethod.GET,
                        base + "/{entityName}",
                        List.of("entityName")
                ),
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.PAGE,
                        RequestMethod.GET,
                        base + "/{entityName}/page",
                        List.of("entityName")
                ),
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.FIND_BY_ID,
                        RequestMethod.GET,
                        base + "/{entityName}/{id}",
                        List.of("entityName", "id")
                ),
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.SAVE,
                        RequestMethod.POST,
                        base + "/{entityName}",
                        List.of("entityName")
                ),
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.UPDATE_BY_ID,
                        RequestMethod.PUT,
                        base + "/{entityName}/{id}",
                        List.of("entityName", "id")
                ),
                new NocodeSpringMvcCrudRoute(
                        NocodeCrudOperation.DELETE_BY_ID,
                        RequestMethod.DELETE,
                        base + "/{entityName}/{id}",
                        List.of("entityName", "id")
                )
        );
    }

    private static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return DEFAULT_BASE_PATH;
        }
        String value = basePath.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
