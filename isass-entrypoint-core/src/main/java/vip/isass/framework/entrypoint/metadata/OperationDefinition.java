// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.metadata;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public record OperationDefinition(
        String operationName,
        String displayName,
        String description,
        int displayOrder,
        HttpMethod httpMethod,
        boolean allowAnonymous,
        Method javaMethod,
        List<ParameterDefinition> parameters,
        Type returnType,
        boolean nocode
) {

    public OperationDefinition {
        parameters = List.copyOf(parameters);
    }
}
