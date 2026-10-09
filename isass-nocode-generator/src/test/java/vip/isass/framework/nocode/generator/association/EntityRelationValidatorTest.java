// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class EntityRelationValidatorTest {

    private static final Map<String, Map<String, Integer>> COLUMNS = Map.of(
            "User", Map.of("id", Types.BIGINT, "account", Types.VARCHAR),
            "UserDetail", Map.of("id", Types.BIGINT, "userId", Types.BIGINT));

    @Test
    void acceptsDirectionalRelationsAcrossDomainModules() {
        assertDoesNotThrow(() -> validate(List.of(relation())));
    }

    @Test
    void rejectsMissingTargetOrKeysBeforeAnyGeneratedFilesAreOverwritten() {
        assertThatThrownBy(() -> validate(List.of(EntityRelationDefinition.one(
                "User", "detail", "Missing", "id", "userId"))))
                .hasMessageContaining("未知实体");
        assertThatThrownBy(() -> validate(List.of(EntityRelationDefinition.one(
                "User", "detail", "UserDetail", "id", "missingId"))))
                .hasMessageContaining("关系键不存在");
    }

    @Test
    void rejectsDuplicateMembersAndPersistentFieldCollisions() {
        assertThatThrownBy(() -> validate(List.of(relation(), relation())))
                .hasMessageContaining("重复");
        assertThatThrownBy(() -> validate(List.of(EntityRelationDefinition.one(
                "User", "account", "UserDetail", "id", "userId"))))
                .hasMessageContaining("持久化字段");
    }

    @Test
    void rejectsIncompatibleKeysAndTreeCascadeOnNonTreeEntities() {
        assertThatThrownBy(() -> validate(List.of(EntityRelationDefinition.one(
                "User", "detail", "UserDetail", "account", "userId"))))
                .hasMessageContaining("类型不兼容");
        assertThatThrownBy(() -> EntityRelationValidator.validate(List.of(), Set.of("User"), COLUMNS))
                .hasMessageContaining("parentId");
    }

    private static EntityRelationDefinition relation() {
        return EntityRelationDefinition.one("User", "userDetail", "UserDetail", "id", "userId");
    }

    private static void validate(List<EntityRelationDefinition> relations) {
        EntityRelationValidator.validate(relations, Set.of(), COLUMNS);
    }
}
