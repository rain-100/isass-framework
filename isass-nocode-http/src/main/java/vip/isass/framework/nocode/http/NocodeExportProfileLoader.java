// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads export profiles from the owning microservice classpath. */
final class NocodeExportProfileLoader {

    private static final String YAML_RESOURCE_PATTERN = "classpath*:export-profiles/*.yaml";
    private static final String YML_RESOURCE_PATTERN = "classpath*:export-profiles/*.yml";

    private final ObjectMapper objectMapper;

    NocodeExportProfileLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Map<String, NocodeExportProfile> load() {
        Map<String, NocodeExportProfile> profiles = new LinkedHashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            for (Resource resource : resources(resolver)) {
                YamlMapFactoryBean yaml = new YamlMapFactoryBean();
                yaml.setResources(resource);
                Map<String, Object> values = yaml.getObject();
                if (values == null || values.isEmpty()) continue;
                NocodeExportProfile profile = objectMapper.convertValue(values, NocodeExportProfile.class);
                validate(profile, resource.getDescription());
                if (profiles.putIfAbsent(profile.getCode(), profile) != null) {
                    throw new IllegalStateException("Duplicate nocode export profile: " + profile.getCode());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load nocode export profiles", exception);
        }
        return Map.copyOf(profiles);
    }

    private Resource[] resources(PathMatchingResourcePatternResolver resolver) throws IOException {
        Resource[] yaml = resolver.getResources(YAML_RESOURCE_PATTERN);
        Resource[] yml = resolver.getResources(YML_RESOURCE_PATTERN);
        Resource[] resources = java.util.Arrays.copyOf(yaml, yaml.length + yml.length);
        System.arraycopy(yml, 0, resources, yaml.length, yml.length);
        return resources;
    }

    private void validate(NocodeExportProfile profile, String source) {
        if (profile.getCode() == null || profile.getCode().isBlank()) {
            throw new IllegalStateException("Missing export profile code: " + source);
        }
        if (profile.getEntities() == null || profile.getEntities().isEmpty()) {
            throw new IllegalStateException("Export profile has no entities: " + profile.getCode());
        }
        for (NocodeExportPlan plan : profile.getEntities()) {
            if (plan.getService() == null || plan.getService().isBlank()
                    || plan.getEntity() == null || plan.getEntity().isBlank()) {
                throw new IllegalStateException("Invalid export entity in profile: " + profile.getCode());
            }
        }
    }
}
