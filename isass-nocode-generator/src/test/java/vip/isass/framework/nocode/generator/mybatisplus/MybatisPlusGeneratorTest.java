// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MybatisPlusGeneratorTest {

    @Test
    void includeTablesRestrictsDatabaseDiscovery() {
        String[] includes = {"bsp_log_request_log"};

        assertThat(MybatisPlusGenerator.isTableSelected("bsp_log_request_log", includes, null)).isTrue();
        assertThat(MybatisPlusGenerator.isTableSelected("bsp_auth_user", includes, null)).isFalse();
    }

    @Test
    void excludeTablesSupportsRegularExpressions() {
        String[] excludes = {"(?i)(.*_)?DATABASECHANGELOG", "(?i)(.*_)?DATABASECHANGELOGLOCK"};

        assertThat(MybatisPlusGenerator.isTableSelected("DATABASECHANGELOG", null, excludes)).isFalse();
        assertThat(MybatisPlusGenerator.isTableSelected("bsp_log_request_log", null, excludes)).isTrue();
    }

    @Test
    void parsesOnlyTheStableContextFromTableName() {
        assertThat(MybatisPlusGenerator.contextOf(
                "bsp_auth_user", new String[]{"bsp_"}))
                .isEqualTo("auth");
        assertThat(MybatisPlusGenerator.contextOf(
                "bsp_auth", new String[]{"bsp_"}))
                .isNull();
    }

    @Test
    void keepsTheWholeEntityNameAfterTheContextSegment() {
        assertThat(MybatisPlusGenerator.entityNameOf(
                "bsp_auth_service_account", new String[]{"bsp_"}))
                .isEqualTo("ServiceAccount");
    }

    @Test
    void combinesStableContextWithDomainAndOptionalSubdomainFromTableRemarks() {
        assertThat(MybatisPlusGenerator.generationScopeOf(
                "bsp_auth_user", "用户 [--domain:identity]", new String[]{"bsp_"}))
                .isEqualTo(new MybatisPlusGenerator.GenerationScope("auth", "identity", null));
        assertThat(MybatisPlusGenerator.generationScopeOf(
                "bsp_auth_role", "角色 [--domain:authorization;--subdomain:role]", new String[]{"bsp_"}))
                .isEqualTo(new MybatisPlusGenerator.GenerationScope("auth", "authorization", "role"));
        assertThat(MybatisPlusGenerator.generationScopeOf(
                "asset_sample_sample_task", "任务 [--domain:workflow]", new String[]{"bsp_"}))
                .isNull();
    }

    @Test
    void placesDomainModulesUnderTheBoundedContextDomainNamespace() {
        assertThat(MybatisPlusGenerator.generationContext("bsp", "attachment", "file"))
                .isEqualTo("bsp.attachment.domain.file");
        assertThat(MybatisPlusGenerator.generationContext(
                "bsp", "auth", "authorization", "serviceaccount"))
                .isEqualTo("bsp.auth.domain.authorization.serviceaccount");
    }

    @Test
    void placesGeneratedCrudServicesInApplicationPackagesGroupedByDomainAndSubdomain() {
        assertThat(MybatisPlusGenerator.applicationServicePackageName(new MybatisPlusGeneratorMeta()
                .setPackageName("vip.isass")
                .setContext("bsp.auth.domain.app")))
                .isEqualTo("vip.isass.bsp.auth.application.app.service");
        assertThat(MybatisPlusGenerator.applicationServicePackageName(new MybatisPlusGeneratorMeta()
                .setPackageName("vip.isass")
                .setContext("bsp.auth.domain.authorization.serviceaccount")))
                .isEqualTo("vip.isass.bsp.auth.application.authorization.serviceaccount.service");
    }

    @Test
    void rejectsApplicationServicePackagesWithoutADomainSegment() {
        MybatisPlusGeneratorMeta meta = new MybatisPlusGeneratorMeta()
                .setPackageName("vip.isass")
                .setContext("bsp.auth");

        assertThatThrownBy(() -> MybatisPlusGenerator.applicationServicePackageName(meta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("domain");
    }

    @Test
    void rejectsOwnedTablesWithoutDomainRemarks() {
        assertThatThrownBy(() -> MybatisPlusGenerator.generationScopeOf(
                "bsp_auth_user", "用户", new String[]{"bsp_"}))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[--domain:{domain}]");
    }
}
