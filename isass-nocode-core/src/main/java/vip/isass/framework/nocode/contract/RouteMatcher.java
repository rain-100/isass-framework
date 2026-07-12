package vip.isass.framework.nocode.contract;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RouteMatcher {

    private RouteMatcher() {
    }

    public static boolean matches(String template, String path) {
        return variables(template, path) != null;
    }

    public static Map<String, String> requireVariables(String template, String path) {
        Map<String, String> variables = variables(template, path);
        if (variables == null) {
            throw new IllegalArgumentException("Path does not match template: " + path + " -> " + template);
        }
        return variables;
    }

    private static Map<String, String> variables(String template, String path) {
        String[] templateParts = trim(template).split("/");
        String[] pathParts = trim(path).split("/");
        if (templateParts.length != pathParts.length) {
            return null;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < templateParts.length; index++) {
            String part = templateParts[index];
            if (part.startsWith("{") && part.endsWith("}")) {
                result.put(part.substring(1, part.length() - 1), pathParts[index]);
            } else if (!part.equals(pathParts[index])) {
                return null;
            }
        }
        return result;
    }

    private static String trim(String value) {
        return value.replaceAll("^/+|/+$", "");
    }
}
