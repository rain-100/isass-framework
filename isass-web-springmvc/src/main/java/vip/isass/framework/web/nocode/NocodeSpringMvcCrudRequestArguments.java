package vip.isass.framework.web.nocode;

import vip.isass.framework.nocode.v3.access.NocodeDeleteOptions;
import vip.isass.framework.nocode.v3.access.NocodeFetchOptions;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;

import java.util.Objects;

/**
 * Arguments parsed from a Spring MVC request before creating a nocode access request.
 */
public record NocodeSpringMvcCrudRequestArguments(
        String entityName,
        Object id,
        NocodeQueryCriteria criteria,
        Object body,
        NocodeFetchOptions fetchOptions,
        NocodeDeleteOptions deleteOptions,
        Class<?> returnType
) {

    public NocodeSpringMvcCrudRequestArguments {
        entityName = requireText(entityName, "entityName");
        criteria = criteria == null ? NocodeQueryCriteria.builder().build() : criteria;
        returnType = returnType == null ? Object.class : returnType;
    }

    public static NocodeSpringMvcCrudRequestArguments query(String entityName, NocodeQueryCriteria criteria) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, null, criteria, null, null, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments query(
            String entityName,
            NocodeQueryCriteria criteria,
            NocodeFetchOptions fetchOptions
    ) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, null, criteria, null, fetchOptions, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments byId(String entityName, Object id) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, id, null, null, null, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments byId(
            String entityName,
            Object id,
            NocodeFetchOptions fetchOptions
    ) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, id, null, null, fetchOptions, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments body(String entityName, Object body) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, null, null, body, null, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments bodyById(String entityName, Object id, Object body) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, id, null, body, null, null, Object.class);
    }

    public static NocodeSpringMvcCrudRequestArguments delete(String entityName, Object id, NocodeDeleteOptions deleteOptions) {
        return new NocodeSpringMvcCrudRequestArguments(entityName, id, null, null, null, deleteOptions, Object.class);
    }

    public NocodeSpringMvcCrudRequestArguments withReturnType(Class<?> returnType) {
        return new NocodeSpringMvcCrudRequestArguments(
                entityName,
                id,
                criteria,
                body,
                fetchOptions,
                deleteOptions,
                returnType
        );
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
