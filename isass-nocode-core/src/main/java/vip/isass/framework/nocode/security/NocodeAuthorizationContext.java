// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.security;

import java.util.List;

/** Resolved nocode invocation details available before the local service method runs. */
public record NocodeAuthorizationContext(
        String service,
        String entity,
        String operation,
        List<Object> arguments
) {
}
