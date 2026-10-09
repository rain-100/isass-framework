// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.sql.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 在写出任何生成文件前检查关系声明，避免拼写错误、成员覆盖或非法关联键破坏生成产物。
 */
public final class EntityRelationValidator {

    private EntityRelationValidator() {
    }

    public static void validate(
            List<EntityRelationDefinition> relations,
            Set<String> treeCascadeEntities,
            Map<String, Map<String, Integer>> entityColumns) {
        Set<String> properties = new HashSet<>();
        for (EntityRelationDefinition relation : relations) {
            Map<String, Integer> source = columns(entityColumns, relation.sourceEntity());
            Map<String, Integer> target = columns(entityColumns, relation.targetEntity());
            String property = relation.sourceEntity() + "." + relation.property();
            if (!properties.add(property) || source.containsKey(relation.property())) {
                throw new IllegalArgumentException("重复或覆盖持久化字段的关系属性: " + property);
            }
            if (source.containsKey("parentId")
                    && Set.of("parent", "children").contains(relation.property())) {
                throw new IllegalArgumentException("关系属性与框架树投影冲突: " + property);
            }
            Integer sourceType = source.get(relation.localKey());
            Integer targetType = target.get(relation.targetKey());
            if (sourceType == null || targetType == null) {
                throw new IllegalArgumentException("关系键不存在: " + property + " ("
                        + relation.localKey() + " -> " + relation.targetKey() + ")");
            }
            if (typeFamily(sourceType) != typeFamily(targetType)) {
                throw new IllegalArgumentException("关系键类型不兼容: " + property);
            }
        }
        for (String entity : treeCascadeEntities) {
            if (!columns(entityColumns, entity).containsKey("parentId")) {
                throw new IllegalArgumentException("树级联删除要求 parentId 字段: " + entity);
            }
        }
    }

    private static Map<String, Integer> columns(
            Map<String, Map<String, Integer>> entityColumns, String entity) {
        Map<String, Integer> columns = entityColumns.get(entity);
        if (columns == null) {
            throw new IllegalArgumentException("关系引用未知实体: " + entity);
        }
        return columns;
    }

    private static int typeFamily(int type) {
        return switch (type) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                    Types.NUMERIC, Types.DECIMAL -> Types.NUMERIC;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR,
                    Types.NCHAR, Types.NVARCHAR, Types.LONGNVARCHAR -> Types.VARCHAR;
            default -> type;
        };
    }
}
