package vip.isass.framework.nocode.v3.generator;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class V3ControllerTemplateTest {

    @Test
    void templateGeneratesStronglyTypedControllerWithStaticEntityPath() throws IOException {
        try (var input = getClass().getResourceAsStream("/v3Template/controller.java.ftl")) {
            assertTrue(input != null, "controller template");
            String template = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(template.contains("implements IV3Controller<V3${entity}, V3${entity}Criteria>"));
            assertTrue(template.contains("${cfg.controllerPrefix}/${entity?uncap_first}"));
            assertTrue(!template.contains("spring.application.name"));
            assertTrue(template.contains("@tag <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>"));
            assertTrue(template.contains("${entity?uncap_first}"));
            assertTrue(template.contains("private V3${entity}Service service"));
        }
    }
}
