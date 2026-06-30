package vip.isass.framework.web.servicedocs;

public interface OpenApiEnhancerSpi {

    String enhance(String rawOpenApiJson);
}
