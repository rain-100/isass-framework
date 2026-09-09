// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableAssociationParserTest {

    @Test
    void infersListAssociationDefaultsAndParsesCascadeDelete() {
        var association = TableAssociationParser.parse("SampleGroup",
                "样片组 [关联表-列表-SampleImage; cascadeDelete=true]").getFirst();

        assertEquals("sampleImages", association.property());
        assertEquals("id", association.localKey());
        assertEquals("sampleGroupId", association.targetKey());
        assertTrue(association.cascadeDelete());
    }

    @Test
    void infersSingleAssociationDefaultsAndTreeMarker() {
        var association = TableAssociationParser.parse("SampleImage",
                "图片 [关联表-单体-SampleGroup] [树结构-cascadeDelete=true]").getFirst();

        assertEquals("sampleGroup", association.property());
        assertEquals("sampleGroupId", association.localKey());
        assertEquals("id", association.targetKey());
        assertFalse(association.cascadeDelete());
        assertTrue(TableAssociationParser.treeCascadeDelete(
                "[树结构-cascadeDelete=true]"));
        assertEquals("图片", TableAssociationParser.description(
                "图片 [关联表-单体-SampleGroup] [树结构-cascadeDelete=true]"));
    }

    @Test
    void parsesDomainAndRemovesItFromTheHumanDescription() {
        String comment = "平台账户 [--domain:identity] [关联表-列表-Role]";

        assertEquals("identity", TableAssociationParser.domain(comment));
        assertNull(TableAssociationParser.subdomain(comment));
        assertEquals("平台账户", TableAssociationParser.description(comment));
    }

    @Test
    void parsesOptionalSubdomainAndRemovesTheWholeMarkerFromDescription() {
        String comment = "角色 [--domain:authorization;--subdomain:role]";

        var metadata = TableAssociationParser.domainMetadata(comment);

        assertEquals("authorization", metadata.domain());
        assertEquals("role", metadata.subdomain());
        assertEquals("角色", TableAssociationParser.description(comment));
    }

    @Test
    void rejectsInvalidOrDuplicateDomains() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> TableAssociationParser.domain("用户 [--domain:Identity]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> TableAssociationParser.domain("用户 [--domain:identity] [--domain:account]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> TableAssociationParser.domainMetadata(
                        "角色 [--domain:authorization;--subdomain:Role]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> TableAssociationParser.domainMetadata(
                        "角色 [--domain:authorization;--subdomain:role;--subdomain:permission]"));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> TableAssociationParser.domainMetadata(
                        "角色 [--domain:authorization;--subdomain:application]"));
    }
}
