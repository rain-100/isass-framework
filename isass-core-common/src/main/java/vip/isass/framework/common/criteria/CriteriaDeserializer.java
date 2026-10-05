// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.common.criteria;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;
import vip.isass.framework.common.criteria.type.IWhereConditionCriteria;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.support.JsonUtil;

import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.List;

/** 普通 JSON 的 Criteria 类型恢复；不承担 URL 或 QUERY 字符串转换。 */
public final class CriteriaDeserializer extends ValueDeserializer<ICriteria<?, ?>> {
    private static final Set<String> REMOVED = Set.of("associationQueries", "associationCriteria", "sourceCriteria",
            "targetType", "relatedQueryVersion");
    private final JavaType declaredType;

    public CriteriaDeserializer() { this(null); }
    private CriteriaDeserializer(JavaType declaredType) { this.declaredType = declaredType; }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        return new CriteriaDeserializer(context.getContextualType());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ICriteria<?, ?> deserialize(JsonParser parser, DeserializationContext context) {
        JsonNode node = context.readTree(parser);
        if (!node.isObject()) throw new IllegalArgumentException("Criteria 必须是 JSON 对象");
        Class<?> declared = declaredType == null ? ICriteria.class : declaredType.getRawClass();
        ICriteria<?, ?> target;
        String name = node.hasNonNull("entityType") ? node.get("entityType").asString() : null;
        if (!declared.isInterface() && !Modifier.isAbstract(declared.getModifiers()) && declared != EmptyCriteria.class) {
            CriteriaEntityTypes.register(declared);
            target = CriteriaEntityTypes.instantiate(declared);
            if (name != null && !name.equals(target.getEntityType())) {
                throw new IllegalArgumentException("entityType 与声明 Criteria 不一致: " + name);
            }
        } else {
            if (name == null) throw new IllegalArgumentException("嵌套 Criteria 缺少 entityType");
            Class<?> entity = CriteriaEntityTypes.requireEntity(name);
            if (node.size() == 1) return EmptyCriteria.of((Class) entity);
            if (declared == EmptyCriteria.class) throw new IllegalArgumentException("EmptyCriteria 只允许 entityType");
            target = CriteriaEntityTypes.newCriteria(entity);
        }
        // 正式条件先恢复，随后便捷筛选只能追加，不能因 JSON 字段顺序覆盖已经登记的条件。
        if (node.has("whereConditions")) {
            if (!(target instanceof IWhereConditionCriteria<?, ?> where)) {
                throw new IllegalArgumentException("当前 Criteria 不支持 whereConditions");
            }
            JavaType conditionsType = context.getTypeFactory().constructCollectionType(List.class, WhereCondition.class);
            where.setWhereConditions(context.readTreeAsValue(node.get("whereConditions"), conditionsType));
            restoreScalarTypes(where.getWhereConditions(), CriteriaEntityTypes.entityClass(target), context);
        }
        for (var entry : node.properties()) {
            String property = entry.getKey();
            if (property.equals("entityType") || property.equals("whereConditions")) continue;
            if (REMOVED.contains(property) || property.startsWith("association.")) {
                throw new IllegalArgumentException("已删除的 Criteria 字段: " + property);
            }
            CriteriaMetadata.Binding binding = CriteriaMetadata.binding(target.getClass(), property);
            if (binding == null) throw new IllegalArgumentException("未知 Criteria 字段: " + property);
            try {
                if (property.equals("loadRelated")) restoreRelatedEntityTypes(entry.getValue(), target);
                Object value = context.readTreeAsValue(entry.getValue(), context.getTypeFactory().constructType(binding.type()));
                binding.set(target, value);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("Criteria 字段绑定失败: " + property, exception);
            }
        }
        return target;
    }

    /** JSON 的 Object 数字不能直接保留为 Integer，否则生成的 Long/枚举便捷 getter 会类型错误。 */
    private static void restoreScalarTypes(List<WhereCondition> conditions, Class<?> entity,
                                          DeserializationContext context) {
        for (WhereCondition condition : conditions) {
            if (condition == null) continue;
            if (condition.getChildren() != null) restoreScalarTypes(condition.getChildren(), entity, context);
            String property = condition.getSourceProperty();
            if (property == null || property.isBlank() || property.contains(".") || condition.getValue() == null
                    || condition.getTargetProperty() != null || condition.getCondition() == null
                    || condition.getCondition().name().startsWith("JSON_")
                    || condition.getCondition().name().startsWith("CONTAINS_")) continue;
            Class<?> valueType = CriteriaMetadata.valueType(entity, property);
            if (valueType == null || valueType == Object.class) continue;
            JavaType type = context.constructType(valueType);
            if (condition.getCondition() == Condition.Membership.IN || condition.getCondition() == Condition.Membership.NOT_IN) {
                type = context.getTypeFactory().constructCollectionType(List.class, type);
            }
            condition.setValue(context.readTreeAsValue(JsonUtil.valueToTree(condition.getValue()), type));
        }
    }

    private static void restoreRelatedEntityTypes(JsonNode relations, ICriteria<?, ?> target) {
        if (relations.isNull()) return;
        if (!relations.isArray()) throw new IllegalArgumentException("loadRelated 必须是数组");
        for (JsonNode relation : relations) {
            JsonNode child = relation.get("criteria");
            if (!(child instanceof ObjectNode object) || object.hasNonNull("entityType")) continue;
            String property = relation.path("property").asString();
            var metadata = CriteriaMetadata.associations(CriteriaEntityTypes.entityClass(target)).stream()
                    .filter(association -> association.property().equals(property))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("未声明的关联属性: " + property));
            object.put("entityType", CriteriaEntityTypes.entityType(metadata.targetType()));
        }
    }

}
