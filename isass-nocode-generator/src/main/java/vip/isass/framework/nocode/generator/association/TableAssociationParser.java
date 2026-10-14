// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.regex.Pattern;

/**
 * 规范表的可读说明，不解析领域归属；模型与关系由 Java 声明。
 */
public final class TableAssociationParser {

    private static final Pattern ASSOCIATION = Pattern.compile(
            "\\[关联表-(列表|单体)-([A-Za-z][A-Za-z0-9]*)(.*?)]", Pattern.DOTALL);
    private static final Pattern TREE_CASCADE = Pattern.compile(
            "\\[树结构-\\s*cascadeDelete\\s*=\\s*true\\s*]", Pattern.CASE_INSENSITIVE);

    private TableAssociationParser() {
    }

    public static String description(String comment) {
        String value = comment == null ? "" : comment;
        value = ASSOCIATION.matcher(value).replaceAll("");
        value = TREE_CASCADE.matcher(value).replaceAll("");
        return value.trim();
    }

}
