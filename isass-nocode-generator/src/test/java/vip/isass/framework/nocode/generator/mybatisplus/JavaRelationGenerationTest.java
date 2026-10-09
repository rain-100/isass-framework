// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vip.isass.framework.nocode.generator.association.EntityRelationDefinition;
import vip.isass.framework.nocode.generator.model.EntityModelDefinition;
import vip.isass.framework.nocode.generator.model.EntityFieldDefinition;
import vip.isass.framework.nocode.generator.model.EntityFieldDefinition.EnumValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JavaRelationGenerationTest {

    @TempDir
    Path output;

    @Test
    void partialGenerationUsesJavaRelationshipsAndResolvesUnselectedTargetDomain() throws Exception {
        MybatisPlusGeneratorMeta meta = database();
        meta.setRelations(List.of(EntityRelationDefinition.one("User", "detail", "UserDetail", "id", "userId")));
        MybatisPlusGenerator.generate(meta);

        String entity = Files.readString(output.resolve("api/src/main/java/vip/example/bsp/auth/domain/identity/domain/model/entity/User.java"));
        assertThat(entity).contains("private UserDetail detail;")
                .contains("import vip.example.bsp.auth.domain.profile.domain.model.entity.UserDetail;")
                .contains("EntityAssociation.one(\"detail\", UserDetail.class,")
                .contains("\"id\", \"userId\", false)")
                .doesNotContain("private UserDetail retiredDetail;");
        assertThat(output.resolve("api/src/main/java/vip/example/bsp/auth/domain/profile/domain/model/entity/UserDetail.java"))
                .doesNotExist();
    }

    @Test
    void invalidRelationFailsBeforeAnyOutputIsWritten() throws Exception {
        MybatisPlusGeneratorMeta meta = database();
        meta.setRelations(List.of(EntityRelationDefinition.one("User", "detail", "UserDetail", "id", "missing")));

        assertThatThrownBy(() -> MybatisPlusGenerator.generate(meta))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("关系键不存在");
        try (var files = Files.list(output)) {
            assertThat(files).isEmpty();
        }
    }

    private MybatisPlusGeneratorMeta database() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE bsp_auth_user (id BIGINT PRIMARY KEY, account VARCHAR(64), tenant_id BIGINT, status INT, preferences VARCHAR(255))");
            statement.execute("COMMENT ON TABLE bsp_auth_user IS '用户 [关联表-单体-UserDetail:property=retiredDetail,localKey=id,targetKey=userId]'");
            statement.execute("CREATE TABLE bsp_auth_user_detail (id BIGINT PRIMARY KEY, user_id BIGINT, real_name VARCHAR(64))");
            statement.execute("COMMENT ON TABLE bsp_auth_user_detail IS '用户详情'");
        }
        return new MybatisPlusGeneratorMeta().setDbType(DbType.H2)
                .setDataSourceUrl(url).setDataSourceUserName("sa").setDataSourcePassword("")
                .setPackageName("vip.example").setContext("bsp")
                .setTablePrefix(new String[]{"bsp_"}).setIncludeTables(new String[]{"bsp_auth_user"})
                .setModels(List.of(EntityModelDefinition.of("User", "identity"),
                        EntityModelDefinition.of("UserDetail", "profile")))
                .setApiOutputDir(output.resolve("api").toString())
                .setServiceOutputDir(output.resolve("service").toString());
    }

    @Test
    void enumsTypesAndTenantExceptionComeOnlyFromJavaDeclarations() throws Exception {
        MybatisPlusGeneratorMeta meta = database();
        meta.setModels(List.of(EntityModelDefinition.of("User", "identity").withoutTenantIsolation()
                        .withFields(EntityFieldDefinition.enumeration("status", new EnumValue(0, "DRAFT", "草稿")),
                                EntityFieldDefinition.type("preferences", "Map<String, Object>")),
                EntityModelDefinition.of("UserDetail", "profile")));
        MybatisPlusGenerator.generate(meta);
        String entity = Files.readString(output.resolve("api/src/main/java/vip/example/bsp/auth/domain/identity/domain/model/entity/User.java"));
        assertThat(entity).contains("enum Status", "DRAFT(0", "Map<String, Object> preferences")
                .doesNotContain("ITenantEntity", "[--domain:", "[枚举--");
    }
}
