// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.metadata;

import vip.isass.framework.entrypoint.authorization.UrlAccessSecurityStrategy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

public record OperationDefinition(
        String operationName,
        String displayName,
        String description,
        int displayOrder,
        HttpMethod httpMethod,
        UrlAccessSecurityStrategy accessStrategy,
        Method javaMethod,
        List<ParameterDefinition> parameters,
        Type returnType,
        boolean nocode
) {

    public OperationDefinition {
        if (accessStrategy == null) {
            accessStrategy = UrlAccessSecurityStrategy.ROLE;
        }
        parameters = List.copyOf(parameters);
    }
}
