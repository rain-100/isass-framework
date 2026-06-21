package vip.isass.framework.apidoc.zyplayer.openapi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZyplayerOpenApiExcludeRulesTest {

    @Test
    void excludesExactPathForAnyMethodWhenRuleHasNoMethod() {
        ZyplayerOpenApiExcludeRules rules = new ZyplayerOpenApiExcludeRules(
                List.of("/error"),
                List.of(),
                List.of());

        assertThat(rules.matches("get", "/error", List.of())).isTrue();
        assertThat(rules.matches("post", "/error", List.of())).isTrue();
        assertThat(rules.matches("get", "/error/detail", List.of())).isFalse();
    }

    @Test
    void excludesExactPathOnlyForMatchingMethodWhenRuleHasMethod() {
        ZyplayerOpenApiExcludeRules rules = new ZyplayerOpenApiExcludeRules(
                List.of("GET /internal/health"),
                List.of(),
                List.of());

        assertThat(rules.matches("get", "/internal/health", List.of())).isTrue();
        assertThat(rules.matches("post", "/internal/health", List.of())).isFalse();
    }

    @Test
    void excludesAntPatternWithOptionalMethodPrefix() {
        ZyplayerOpenApiExcludeRules rules = new ZyplayerOpenApiExcludeRules(
                List.of(),
                List.of("/actuator/**", "POST /internal/**"),
                List.of());

        assertThat(rules.matches("get", "/actuator/health", List.of())).isTrue();
        assertThat(rules.matches("get", "/actuator", List.of())).isTrue();
        assertThat(rules.matches("post", "/internal/sync", List.of())).isTrue();
        assertThat(rules.matches("get", "/internal/sync", List.of())).isFalse();
    }

    @Test
    void excludesSingleSegmentWildcardAndSingleCharacterWildcard() {
        ZyplayerOpenApiExcludeRules rules = new ZyplayerOpenApiExcludeRules(
                List.of(),
                List.of("/internal/*/detail", "/file/?/preview"),
                List.of());

        assertThat(rules.matches("get", "/internal/order/detail", List.of())).isTrue();
        assertThat(rules.matches("get", "/internal/order/sub/detail", List.of())).isFalse();
        assertThat(rules.matches("get", "/file/a/preview", List.of())).isTrue();
        assertThat(rules.matches("get", "/file/ab/preview", List.of())).isFalse();
    }

    @Test
    void excludesFrameworkControllerNames() {
        ZyplayerOpenApiExcludeRules rules = new ZyplayerOpenApiExcludeRules(
                List.of(),
                List.of(),
                List.of("vip.isass.framework.web.error.IsassErrorController"));

        assertThat(rules.matches("get", "/error", List.of("vip.isass.framework.web.error.IsassErrorController"))).isTrue();
        assertThat(rules.matches("get", "/error", List.of("BusinessController"))).isFalse();
    }
}
