package vip.isass.framework.apidoc.zyplayer.openapi;

import org.springframework.util.AntPathMatcher;
import vip.isass.framework.apidoc.zyplayer.ZyplayerText;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Rain
 */
public class ZyplayerOpenApiExcludeRules {

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS");

    private final List<Rule> paths;

    private final List<Rule> pathPatterns;

    private final List<String> controllers;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ZyplayerOpenApiExcludeRules(List<String> paths, List<String> pathPatterns, List<String> controllers) {
        this.paths = normalizeRules(paths);
        this.pathPatterns = normalizeRules(pathPatterns);
        this.controllers = normalizeControllers(controllers);
    }

    public boolean matches(String method, String path, List<String> operationControllers) {
        String normalizedMethod = normalizeMethod(method);
        String normalizedPath = normalizePath(path);
        if (paths.stream().anyMatch(rule -> rule.matches(normalizedMethod, normalizedPath))) {
            return true;
        }
        if (pathPatterns.stream().anyMatch(rule -> rule.matchesPattern(pathMatcher, normalizedMethod, normalizedPath))) {
            return true;
        }
        return operationControllers.stream()
                .filter(ZyplayerText::hasText)
                .map(String::trim)
                .anyMatch(controllers::contains);
    }

    private List<Rule> normalizeRules(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(ZyplayerText::hasText)
                .map(Rule::parse)
                .toList();
    }

    private List<String> normalizeControllers(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(ZyplayerText::hasText)
                .map(String::trim)
                .toList();
    }

    private String normalizeMethod(String method) {
        return ZyplayerText.hasText(method) ? method.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String normalizePath(String path) {
        if (!ZyplayerText.hasText(path)) {
            return "/";
        }
        String value = path.trim();
        return value.startsWith("/") ? value : "/" + value;
    }

    private record Rule(String method, String path) {

        private static Rule parse(String value) {
            String trimmed = value.trim();
            int spaceIndex = trimmed.indexOf(' ');
            if (spaceIndex > 0) {
                String first = trimmed.substring(0, spaceIndex).trim().toUpperCase(Locale.ROOT);
                if (METHODS.contains(first)) {
                    return new Rule(first, normalizeRulePath(trimmed.substring(spaceIndex + 1)));
                }
            }
            return new Rule(null, normalizeRulePath(trimmed));
        }

        private boolean matches(String operationMethod, String operationPath) {
            return methodMatches(operationMethod) && path.equals(operationPath);
        }

        private boolean matchesPattern(AntPathMatcher pathMatcher, String operationMethod, String operationPath) {
            return methodMatches(operationMethod) && pathMatcher.match(path, operationPath);
        }

        private boolean methodMatches(String operationMethod) {
            return method == null || method.equals(operationMethod);
        }

        private static String normalizeRulePath(String path) {
            String value = path.trim();
            return value.startsWith("/") ? value : "/" + value;
        }
    }
}
