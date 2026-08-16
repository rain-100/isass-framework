// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.http;

import java.net.URI;

public interface HttpEndpointResolver {
    URI resolve(String serviceName);
}
