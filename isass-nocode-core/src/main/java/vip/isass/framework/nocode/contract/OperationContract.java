// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.contract;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OperationContract(
        String name,
        HttpMethod httpMethod,
        String path,
        int order,
        boolean idempotent,
        List<ParameterContract> parameters,
        String returnJavaType,
        String description
) {
    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{([^}]+)}");

    public OperationContract {
        name = requireText(name, "name");
        httpMethod = Objects.requireNonNull(httpMethod, "httpMethod");
        path = normalizePath(path);
        if (order < 0) {
            throw new IllegalArgumentException("order must be positive");
        }
        parameters = List.copyOf(parameters == null ? List.of() : parameters);
        returnJavaType = requireText(returnJavaType, "returnJavaType");
        description = description == null ? "" : description;
        validatePathVariables(name, path, parameters);
    }

    private static void validatePathVariables(
            String operationName,
            String path,
            List<ParameterContract> parameters
    ) {
        Matcher matcher = PATH_VARIABLE.matcher(path);
        while (matcher.find()) {
            String variable = matcher.group(1);
            boolean found = parameters.stream().anyMatch(parameter ->
                    parameter.source() == ParameterSource.PATH
                            && parameter.name().equals(variable));
            if (!found) {
                throw new IllegalArgumentException(
                        "Path variable '" + variable + "' has no matching parameter in " + operationName);
            }
        }
    }

    private static String normalizePath(String value) {
        String path = requireText(value, "path");
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
