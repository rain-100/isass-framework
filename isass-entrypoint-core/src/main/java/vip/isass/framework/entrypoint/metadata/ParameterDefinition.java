// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.metadata;

import java.lang.reflect.Type;

public record ParameterDefinition(
        int index,
        String name,
        ParameterSource source,
        Type javaType,
        boolean objectQuery
) {
}
