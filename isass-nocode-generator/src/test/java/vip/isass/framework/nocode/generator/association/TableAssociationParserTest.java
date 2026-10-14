// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableAssociationParserTest {

    @Test
    void removesRetiredRelationMarkersOnlyFromDisplayDescriptions() {
        assertEquals("图片", TableAssociationParser.description(
                "图片 [关联表-单体-SampleGroup] [树结构-cascadeDelete=true]"));
    }

    @Test
    void preservesHumanDescriptionsAndOrdinaryBrackets() {
        assertEquals("平台账户 [全局身份]", TableAssociationParser.description("  平台账户 [全局身份]  "));
        assertEquals("角色", TableAssociationParser.description("角色"));
    }

    @Test
    void normalizesMissingOrBlankDescriptions() {
        assertEquals("", TableAssociationParser.description(null));
        assertEquals("", TableAssociationParser.description(" \t\n "));
    }
}
