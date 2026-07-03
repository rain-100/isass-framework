package vip.isass.framework.nocode.v3.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.nocode.v3.criteria.IV3Criteria;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IV3ControllerResponseTest {

    @Test
    void everyHttpEndpointReturnsResp() {
        Arrays.stream(IV3Controller.class.getDeclaredMethods())
                .filter(this::isHttpEndpoint)
                .forEach(method -> assertEquals(Resp.class, method.getReturnType(), method.getName()));
    }

    @Test
    void criteriaParametersAreModelAttributes() {
        Arrays.stream(IV3Controller.class.getDeclaredMethods())
                .filter(this::isHttpEndpoint)
                .flatMap(method -> Arrays.stream(method.getParameters()))
                .filter(this::isCriteria)
                .forEach(parameter -> assertTrue(parameter.isAnnotationPresent(ModelAttribute.class),
                        parameter.getDeclaringExecutable().getName()));
    }

    @Test
    void endpointsDeclareSmartDocOrderInAddDeleteUpdateQueryGroups() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/vip/isass/framework/nocode/v3/controller/IV3Controller.java"));
        Matcher matcher = Pattern.compile(
                "@order\\s+(\\d+).*?default\\s+Resp<[^\\n]+?\\s+(\\w+)\\(",
                Pattern.DOTALL).matcher(source);
        java.util.Map<String, Integer> orders = new java.util.HashMap<>();
        while (matcher.find()) {
            orders.put(matcher.group(2), Integer.parseInt(matcher.group(1)));
        }

        assertEquals(36, orders.size());
        assertTrue(orders.get("add") < orders.get("updateById"));
        assertTrue(orders.get("updateById") < orders.get("getById"));
        assertTrue(orders.get("getById") < orders.get("deleteById"));
    }

    private boolean isCriteria(Parameter parameter) {
        return IV3Criteria.class.isAssignableFrom(parameter.getType());
    }

    private boolean isHttpEndpoint(Method method) {
        return Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType()
                        .isAnnotationPresent(RequestMapping.class));
    }
}
