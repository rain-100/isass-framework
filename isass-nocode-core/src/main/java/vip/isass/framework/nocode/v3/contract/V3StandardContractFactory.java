package vip.isass.framework.nocode.v3.contract;

import vip.isass.framework.nocode.v3.service.IV3Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    private V3StandardContractFactory() {
    }

    public static List<V3OperationContract> operations(
            String entityJavaType,
            String criteriaJavaType
    ) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Method method : IV3Service.class.getMethods()) {
            String prefix = constantPrefix(method.getName());
            if (hasField(prefix + "_OPERATOR") && hasField(prefix + "_URI_SECOND_PART")) {
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
        String prefix = constantPrefix(method.getName());
        V3HttpMethod httpMethod = V3HttpMethod.valueOf(stringField(prefix + "_OPERATOR"));
        String path = stringField(prefix + "_URI_SECOND_PART").replaceFirst("^/v3", "");
        if (path.isEmpty()) {
            path = "/";
        }
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

    private static String constantPrefix(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
    }

    private static boolean hasField(String name) {
        return Arrays.stream(IV3Service.class.getFields()).anyMatch(field -> field.getName().equals(name));
    }

    private static String stringField(String name) {
        try {
            Field field = IV3Service.class.getField(name);
            return (String) field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read IV3Service metadata: " + name, exception);
        }
    }
}
