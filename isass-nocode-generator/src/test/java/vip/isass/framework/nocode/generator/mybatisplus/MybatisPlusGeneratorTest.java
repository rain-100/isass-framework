// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.generator.model.EntityModelDefinition;
import java.util.List;

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
    void combinesStableTableContextWithJavaModelOwnership() {
        var meta = new MybatisPlusGeneratorMeta().setTablePrefix(new String[]{"bsp_"})
                .setModels(List.of(EntityModelDefinition.of("User", "identity"),
                        EntityModelDefinition.of("Role", "authorization", "role")));
        assertThat(MybatisPlusGenerator.modelScopeOf("bsp_auth_user", meta))
                .isEqualTo(new MybatisPlusGenerator.GenerationScope("auth", "identity", null));
        assertThat(MybatisPlusGenerator.modelScopeOf("bsp_auth_role", meta))
                .isEqualTo(new MybatisPlusGenerator.GenerationScope("auth", "authorization", "role"));
        assertThat(MybatisPlusGenerator.modelScopeOf("asset_sample_sample_task", meta))
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
    void rejectsTablesWithoutAnExplicitModelDeclaration() {
        assertThatThrownBy(() -> MybatisPlusGenerator.modelScopeOf("bsp_auth_user",
                new MybatisPlusGeneratorMeta().setTablePrefix(new String[]{"bsp_"})))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EntityModelDefinition");
    }
}
