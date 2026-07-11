package vip.isass.framework.nocode.v3.contract;

import vip.isass.framework.nocode.v3.service.IV3Service;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class V3StandardContractFactory {

    private static final List<String> OPERATION_ORDER = List.of(
            "add", "addBatch", "addBatchByBatchSize",
            "addIfAbsentByCriteria", "addIfAbsentByColumns",
            "addBatchIfAbsentByCriteria", "addBatchIfAbsentByColumns",
            "addOrUpdateByCriteria", "addOrUpdateByColumns", "addOrUpdateBatchByColumns",
            "updateById", "updateAllColumnsById", "updateByIdOrException",
            "updateByCriteria", "updateByCriteriaOrException", "batchSave",
            "getById", "getByIdOrException", "getByCriteria",
            "getByCriteriaOrWarn", "getByCriteriaOrException",
            "findByCriteria", "findPageByCriteria", "findAll",
            "countByCriteria", "countAll",
            "isPresentById", "isPresentByColumn", "isPresentByCriteria",
            "isAbsentByColumn", "isAbsentByCriteria",
            "exceptionIfPresentByCriteria", "exceptionIfAbsentByCriteria",
            "deleteById", "deleteByIds", "deleteByCriteria"
    );
    private static final Map<String, HttpRoute> ROUTES = Map.ofEntries(
            route("add", "POST", "/"), route("addBatch", "POST", "/batch"),
            route("addBatchByBatchSize", "POST", "/batch/batchSize/{batchSize}"),
            route("addIfAbsentByCriteria", "POST", "/absent/criteria"),
            route("addIfAbsentByColumns", "POST", "/absent/{uniqueColumns}"),
            route("addBatchIfAbsentByCriteria", "POST", "/batch/absent/criteria"),
            route("addBatchIfAbsentByColumns", "POST", "/batch/absent/{uniqueColumns}"),
            route("addOrUpdateByCriteria", "POST", "/add-update/criteria"),
            route("addOrUpdateByColumns", "POST", "/add-update/{uniqueColumns}"),
            route("addOrUpdateBatchByColumns", "POST", "/add-update/batch/{uniqueColumns}"),
            route("updateById", "PUT", "/"), route("updateAllColumnsById", "PUT", "/allColumns"),
            route("updateByIdOrException", "PUT", "/exception"), route("updateByCriteria", "PUT", "/criteria"),
            route("updateByCriteriaOrException", "PUT", "/criteria/exception"), route("batchSave", "POST", "/batchSave"),
            route("getById", "GET", "/{id}"), route("getByIdOrException", "GET", "/exception/{id}"),
            route("getByCriteria", "GET", "/1/criteria"), route("getByCriteriaOrWarn", "GET", "/warn/criteria"),
            route("getByCriteriaOrException", "GET", "/exception/criteria"), route("findByCriteria", "GET", "/criteria"),
            route("findPageByCriteria", "GET", "/page"), route("findAll", "GET", "/all"),
            route("countByCriteria", "GET", "/count/criteria"), route("countAll", "GET", "/count/all"),
            route("isPresentById", "GET", "/present/{id}"), route("isPresentByColumn", "GET", "/present/{columnName}/{value}"),
            route("isPresentByCriteria", "GET", "/present/criteria"), route("isAbsentByColumn", "GET", "/absent/{columnName}/{value}"),
            route("isAbsentByCriteria", "GET", "/absent/criteria"), route("exceptionIfPresentByCriteria", "GET", "/exception-if-present/criteria"),
            route("exceptionIfAbsentByCriteria", "GET", "/exception-if-absent/criteria"),
            route("deleteById", "DELETE", "/id/{id}"), route("deleteByIds", "DELETE", "/{ids}"), route("deleteByCriteria", "DELETE", "/criteria")
    );

    private V3StandardContractFactory() {
    }

    public static List<V3OperationContract> operations(
            String entityJavaType,
            String criteriaJavaType
    ) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Method method : IV3Service.class.getMethods()) {
            if (ROUTES.containsKey(method.getName())) {
                methods.put(method.getName(), method);
            }
        }
        List<V3OperationContract> operations = new ArrayList<>();
        for (String name : OPERATION_ORDER) {
            Method method = methods.get(name);
            if (method == null) {
                throw new IllegalStateException("IV3Service operation metadata is missing: " + name);
            }
            operations.add(operation(method, entityJavaType, criteriaJavaType));
        }
        return List.copyOf(operations);
    }

    private static V3OperationContract operation(
            Method method,
            String entityJavaType,
            String criteriaJavaType
    ) {
        HttpRoute route = ROUTES.get(method.getName());
        V3HttpMethod httpMethod = route.method();
        String path = route.path();
        List<V3ParameterContract> parameters = new ArrayList<>();
        Parameter[] methodParameters = method.getParameters();
        Type[] types = method.getGenericParameterTypes();
        for (int index = 0; index < methodParameters.length; index++) {
            String name = methodParameters[index].getName();
            String javaType = substitute(types[index].getTypeName(), entityJavaType, criteriaJavaType);
            V3ParameterSource source;
            if (path.contains("{" + name + "}")) {
                source = V3ParameterSource.PATH;
            } else if (name.toLowerCase().contains("criteria")
                    || httpMethod == V3HttpMethod.GET
                    || httpMethod == V3HttpMethod.DELETE) {
                source = V3ParameterSource.QUERY;
            } else {
                source = V3ParameterSource.BODY;
            }
            parameters.add(new V3ParameterContract(name, javaType, source,
                    source == V3ParameterSource.PATH, ""));
        }
        int order = order(method.getName());
        return new V3OperationContract(
                method.getName(),
                httpMethod,
                path,
                order,
                httpMethod == V3HttpMethod.GET,
                parameters,
                substitute(method.getGenericReturnType().getTypeName(), entityJavaType, criteriaJavaType),
                method.getName());
    }

    private static int order(String operationName) {
        int index = OPERATION_ORDER.indexOf(operationName);
        if (index < 10) {
            return 101 + index;
        }
        if (index < 16) {
            return 201 + index - 10;
        }
        if (index < 33) {
            return 301 + index - 16;
        }
        return 401 + index - 33;
    }

    private static String substitute(String type, String entity, String criteria) {
        return type.replaceAll("(?<![\\w.])E(?![\\w])", entity)
                .replaceAll("(?<![\\w.])C(?![\\w])", criteria);
    }

    private static Map.Entry<String, HttpRoute> route(String operation, String method, String path) {
        return Map.entry(operation, new HttpRoute(V3HttpMethod.valueOf(method), path));
    }

    private record HttpRoute(V3HttpMethod method, String path) {}
}
