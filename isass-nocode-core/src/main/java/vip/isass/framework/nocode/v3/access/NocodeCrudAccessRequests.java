package vip.isass.framework.nocode.v3.access;

import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;

import java.util.Map;

/**
 * Factory methods for standard nocode v3 CRUD access requests.
 */
public final class NocodeCrudAccessRequests {

    public static final String ARG_ID = "id";
    public static final String ARG_BODY = "body";
    public static final String ARG_CRITERIA = "criteria";
    public static final String ARG_DELETE_OPTIONS = "deleteOptions";
    public static final String ARG_FETCH_OPTIONS = "fetchOptions";

    private NocodeCrudAccessRequests() {
    }

    public static NocodeAccessRequest findById(String entityName, Object id, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.FIND_BY_ID, Map.of(ARG_ID, id), returnType);
    }

    public static NocodeAccessRequest findById(
            String entityName,
            Object id,
            NocodeFetchOptions fetchOptions,
            Class<?> returnType
    ) {
        return request(
                entityName,
                NocodeCrudOperation.FIND_BY_ID,
                Map.of(ARG_ID, id, ARG_FETCH_OPTIONS, fetchOptions == null ? NocodeFetchOptions.none() : fetchOptions),
                returnType
        );
    }

    public static NocodeAccessRequest page(String entityName, NocodeQueryCriteria criteria, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.PAGE, Map.of(ARG_CRITERIA, criteria), returnType);
    }

    public static NocodeAccessRequest page(
            String entityName,
            NocodeQueryCriteria criteria,
            NocodeFetchOptions fetchOptions,
            Class<?> returnType
    ) {
        return request(
                entityName,
                NocodeCrudOperation.PAGE,
                Map.of(ARG_CRITERIA, criteria, ARG_FETCH_OPTIONS, fetchOptions == null ? NocodeFetchOptions.none() : fetchOptions),
                returnType
        );
    }

    public static NocodeAccessRequest list(String entityName, NocodeQueryCriteria criteria, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.LIST, Map.of(ARG_CRITERIA, criteria), returnType);
    }

    public static NocodeAccessRequest list(
            String entityName,
            NocodeQueryCriteria criteria,
            NocodeFetchOptions fetchOptions,
            Class<?> returnType
    ) {
        return request(
                entityName,
                NocodeCrudOperation.LIST,
                Map.of(ARG_CRITERIA, criteria, ARG_FETCH_OPTIONS, fetchOptions == null ? NocodeFetchOptions.none() : fetchOptions),
                returnType
        );
    }

    public static NocodeAccessRequest save(String entityName, Object body, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.SAVE, Map.of(ARG_BODY, body), returnType);
    }

    public static NocodeAccessRequest updateById(String entityName, Object id, Object body, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.UPDATE_BY_ID, Map.of(ARG_ID, id, ARG_BODY, body), returnType);
    }

    public static NocodeAccessRequest deleteById(String entityName, Object id, Class<?> returnType) {
        return request(entityName, NocodeCrudOperation.DELETE_BY_ID, Map.of(ARG_ID, id), returnType);
    }

    public static NocodeAccessRequest deleteById(
            String entityName,
            Object id,
            NocodeDeleteOptions deleteOptions,
            Class<?> returnType
    ) {
        return request(
                entityName,
                NocodeCrudOperation.DELETE_BY_ID,
                Map.of(ARG_ID, id, ARG_DELETE_OPTIONS, deleteOptions == null ? NocodeDeleteOptions.none() : deleteOptions),
                returnType
        );
    }

    private static NocodeAccessRequest request(
            String entityName,
            NocodeCrudOperation operation,
            Map<String, Object> arguments,
            Class<?> returnType
    ) {
        return new NocodeAccessRequest(entityName, operation.getOperationName(), arguments, returnType, null);
    }
}
