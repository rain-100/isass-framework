package vip.isass.framework.apidoc.zyplayer;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerClientOperations;
import vip.isass.framework.apidoc.zyplayer.client.ZyplayerOpenApiClient;
import vip.isass.framework.apidoc.zyplayer.openapi.ZyplayerOpenApiDocsCollector;
import vip.isass.framework.apidoc.zyplayer.openapi.ZyplayerOpenApiDocumentConverter;
import vip.isass.framework.apidoc.zyplayer.openapi.ZyplayerOpenApiExcludeRules;
import vip.isass.framework.apidoc.zyplayer.sync.ZyplayerDocSyncService;
import vip.isass.framework.web.servicedocs.ServiceDocsScanner;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rain
 */
@AutoConfiguration
@EnableConfigurationProperties(ZyplayerApidocProperties.class)
@ConditionalOnProperty(prefix = "isass.apidoc.zyplayer", name = "enabled", havingValue = "true")
public class ZyplayerApidocAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerServiceDescriptor zyplayerServiceDescriptor(
            Environment environment,
            ZyplayerApidocProperties properties) {
        String applicationName = environment.getProperty("spring.application.name", "application");
        String serviceNameCn = environment.getProperty("info.service-name-cn", applicationName);
        String version = firstText(
                environment.getProperty("info.version"),
                environment.getProperty("git.build.version"),
                properties.getVersion());
        return new ZyplayerServiceDescriptor(applicationName, serviceNameCn, version, properties.getGroupName());
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper zyplayerObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerClientOperations zyplayerClientOperations(
            ZyplayerApidocProperties properties,
            ObjectMapper objectMapper) {
        return new ZyplayerOpenApiClient(properties.getBaseUrl(), properties.getApiKey(), properties.getPrivateKey(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerDocSyncService zyplayerDocSyncService(
            ZyplayerClientOperations client,
            ObjectMapper objectMapper) {
        return new ZyplayerDocSyncService(client, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerOpenApiExcludeRules zyplayerOpenApiExcludeRules(ZyplayerApidocProperties properties) {
        List<String> paths = new ArrayList<>(List.of("/error"));
        paths.addAll(properties.getExcludePaths());
        List<String> patterns = new ArrayList<>(List.of("/actuator/**", "/*/actuator/**"));
        patterns.addAll(properties.getExcludePathPatterns());
        List<String> controllers = new ArrayList<>(List.of(
                "vip.isass.framework.web.error.IsassErrorController",
                "IsassErrorController"));
        controllers.addAll(properties.getExcludeControllers());
        return new ZyplayerOpenApiExcludeRules(paths, patterns, controllers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerOpenApiDocumentConverter zyplayerOpenApiDocumentConverter(
            ObjectMapper objectMapper,
            ZyplayerOpenApiExcludeRules excludeRules) {
        return new ZyplayerOpenApiDocumentConverter(objectMapper, excludeRules);
    }

    @Bean
    @ConditionalOnMissingBean
    public ZyplayerOpenApiDocsCollector zyplayerOpenApiDocsCollector(
            Environment environment,
            ZyplayerApidocProperties properties,
            ZyplayerOpenApiDocumentConverter converter,
            ResourceLoader resourceLoader) {
        return new ZyplayerOpenApiDocsCollector(
                properties,
                converter,
                () -> firstText(environment.getProperty("local.server.port"), environment.getProperty("server.port"), "8080"),
                location -> readResource(resourceLoader, location));
    }

    @Bean
    @ConditionalOnBean(ServiceDocsScanner.class)
    @ConditionalOnMissingBean
    public ZyplayerServiceDocsCollector zyplayerServiceDocsCollector(
            ServiceDocsScanner serviceDocsScanner,
            ZyplayerOpenApiDocsCollector openApiDocsCollector) {
        return new ZyplayerServiceDocsCollector(serviceDocsScanner, openApiDocsCollector);
    }

    @Bean
    @ConditionalOnBean(ZyplayerServiceDocsCollector.class)
    @ConditionalOnMissingBean
    public ZyplayerApidocRunner zyplayerApidocRunner(
            ZyplayerServiceDescriptor descriptor,
            ZyplayerServiceDocsCollector collector,
            ZyplayerDocSyncService syncService,
            ZyplayerApidocProperties properties) {
        return new ZyplayerApidocRunner(descriptor, collector, syncService, properties);
    }

    private String firstText(String... values) {
        return ZyplayerText.firstText("0.0.0", values);
    }

    private String readResource(ResourceLoader resourceLoader, String location) {
        try {
            Resource resource = resourceLoader.getResource(location);
            if (!resource.exists()) {
                return null;
            }
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
