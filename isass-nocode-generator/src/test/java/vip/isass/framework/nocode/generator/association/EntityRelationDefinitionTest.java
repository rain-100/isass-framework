// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.generator.association.GeneratorAssociation.CollectionType;
import vip.isass.framework.nocode.generator.association.GeneratorAssociation.Kind;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityRelationDefinitionTest {

    @Test
    void manyKeepsItsCollectionDefault() {
        EntityRelationDefinition definition = EntityRelationDefinition.many(
                "Work", "storyboardShots", "StoryboardShot", "id", "workId");

        assertThat(definition.collectionType()).isEqualTo(CollectionType.COLLECTION);
        assertThat(definition.association().javaType()).isEqualTo("Collection<StoryboardShot>");
        assertThat(definition.cascadeDelete()).isFalse();
    }

    @Test
    void listTypeSurvivesFixedConditionsAndMetadataConversion() {
        EntityRelationDefinition definition = EntityRelationDefinition.list(
                "Work", "storyboardShots", "StoryboardShot", "id", "workId", true)
                .where("status", "1").where("status", "2");

        GeneratorAssociation association = definition.association();

        assertThat(definition.collectionType()).isEqualTo(CollectionType.LIST);
        assertThat(association.collectionType()).isEqualTo(CollectionType.LIST);
        assertThat(association.kind()).isEqualTo(Kind.MANY);
        assertThat(association.javaType()).isEqualTo("List<StoryboardShot>");
        assertThat(association.property()).isEqualTo("storyboardShots");
        assertThat(association.localKey()).isEqualTo("id");
        assertThat(association.targetKey()).isEqualTo("workId");
        assertThat(association.cascadeDelete()).isTrue();
        assertThat(association.modifiers()).isEqualTo(".where(\"status\", \"2\")");
        assertThat(EntityRelationDefinition.list(
                "Work", "storyboardShots", "StoryboardShot", "id", "workId").cascadeDelete())
                .isFalse();
    }

    @Test
    void oneKeepsItsEntityTypeWithoutACollectionType() {
        EntityRelationDefinition definition = EntityRelationDefinition.one(
                "Work", "projectRule", "ProjectRule", "projectInfoId", "projectInfoId")
                .where("status", "1");

        assertThat(definition.collectionType()).isNull();
        assertThat(definition.association().javaType()).isEqualTo("ProjectRule");
        assertThat(definition.association().kind()).isEqualTo(Kind.ONE);
    }

    @Test
    void existingConstructorsKeepTheirDefaultTypesAndConditions() {
        EntityRelationDefinition definition = new EntityRelationDefinition(
                "Work", "storyboardShots", "StoryboardShot", Kind.MANY,
                "id", "workId", false, Map.of("status", "1"));
        GeneratorAssociation association = new GeneratorAssociation(
                "storyboardShots", "StoryboardShot", Kind.MANY, "id", "workId", false);

        assertThat(definition.association().javaType()).isEqualTo("Collection<StoryboardShot>");
        assertThat(definition.fixedValues()).containsEntry("status", "1");
        assertThat(association.javaType()).isEqualTo("Collection<StoryboardShot>");
        assertThat(new GeneratorAssociation("projectRule", "ProjectRule", Kind.ONE,
                "projectInfoId", "projectInfoId", false).javaType()).isEqualTo("ProjectRule");
    }

    @Test
    void rejectsCollectionTypesOnSingleRelations() {
        assertThatThrownBy(() -> new EntityRelationDefinition(
                "Work", "projectRule", "ProjectRule", Kind.ONE,
                "projectInfoId", "projectInfoId", false, Map.of(), CollectionType.LIST))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("单个关联");
        assertThatThrownBy(() -> new GeneratorAssociation("projectRule", "ProjectRule", Kind.ONE,
                "projectInfoId", "projectInfoId", false, Map.of(), CollectionType.COLLECTION))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("单个关联");
    }

    @Test
    void rejectsMissingTypesOnMultiValuedRelations() {
        assertThatThrownBy(() -> new EntityRelationDefinition(
                "Work", "storyboardShots", "StoryboardShot", Kind.MANY,
                "id", "workId", false, Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("多项关联");
        assertThatThrownBy(() -> new GeneratorAssociation("storyboardShots", "StoryboardShot", Kind.MANY,
                "id", "workId", false, Map.of(), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("多项关联");
    }
}
