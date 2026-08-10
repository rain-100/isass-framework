// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import java.net.URI;

@FunctionalInterface
public interface HttpEndpointResolver {

    URI resolve(String service);
}
