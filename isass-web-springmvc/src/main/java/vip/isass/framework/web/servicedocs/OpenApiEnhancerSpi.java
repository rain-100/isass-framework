// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.servicedocs;

public interface OpenApiEnhancerSpi {

    String enhance(String rawOpenApiJson);
}
