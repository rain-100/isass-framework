// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

import org.springframework.util.StringUtils;
import vip.isass.framework.web.security.PermitUrlProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Exposes the OpenAPI contract endpoints so API document aggregation can read them.
 */
public class OpenApiPermitUrlProvider implements PermitUrlProvider {

    private final String applicationName;

    public OpenApiPermitUrlProvider(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public Collection<String> getUrls() {
        List<String> urls = new ArrayList<>(List.of("/v3/api-docs"));
        if (StringUtils.hasText(applicationName)) {
            urls.add("/" + applicationName + "/v3/api-docs");
        }
        return urls;
    }
}
