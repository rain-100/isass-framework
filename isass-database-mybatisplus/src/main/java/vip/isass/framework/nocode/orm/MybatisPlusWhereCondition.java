// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.orm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import tools.jackson.core.JacksonException;
import lombok.SneakyThrows;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import vip.isass.framework.nocode.criteria.WhereCondition;
import vip.isass.framework.nocode.criteria.impl.type.Condition;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.common.support.BeanProviderUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Rain
 */
public class MybatisPlusWhereCondition {

    @SuppressWarnings("unchecked")
    public static void apply(WhereCondition whereCondition, AbstractWrapper wrapper) {
        Class<?> entityClass = wrapper.getEntityClass();
        Assert.notNull(entityClass, "Wrapper 未设置实体类型");
        String columnName = whereCondition.getCondition() == Condition.OR
                ? null
                : EntityPropertyColumnResolver.resolve(entityClass, whereCondition.getPropertyName());
        switch (whereCondition.getCondition()) {
            case OR:
                wrapper.or();
                break;
            case EQUAL:
                wrapper.eq(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case NOT_EQUAL:
                wrapper.ne(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case IN:

                if (whereCondition.getValue() == null) {
                    break;
                }
                if (whereCondition.getValue() instanceof Collection) {
                    wrapper.in(columnName, ((Collection) whereCondition.getValue()).toArray());
                } else {
                    wrapper.in(columnName, whereCondition.getValue());
                }
                break;
            case NOT_IN:
                if (whereCondition.getValue() == null) {
                    break;
                }
                if (whereCondition.getValue() instanceof Collection) {
                    wrapper.notIn(columnName, ((Collection) whereCondition.getValue()).toArray());
                } else {
                    wrapper.notIn(columnName, whereCondition.getValue());
                }
                break;
            case IS_NULL:
                if (StrUtil.isNotBlank(columnName)) {
                    wrapper.isNull(columnName);
                }
                break;
            case IS_NOT_NULL:
                if (StrUtil.isNotBlank(columnName)) {
                    wrapper.isNotNull(columnName);
                }
                break;
            case IS_EMPTY:
                wrapper.eq(columnName, "");
                break;
            case IS_NOT_EMPTY:
                wrapper.ne(columnName, "");
                break;
            case GREATER_THAN:
                wrapper.gt(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case GREATER_THAN_EQUAL:
                wrapper.ge(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case LESS_THAN:
                wrapper.lt(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case LESS_THAN_EQUAL:
                wrapper.le(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case START_WITH:
                wrapper.likeRight(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case LIKE:
                wrapper.like(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case NOT_LIKE:
                wrapper.notLike(whereCondition.getValue() != null, columnName, whereCondition.getValue());
                break;
            case CONTAINS_ALL:
                wrapper.apply(
                        whereCondition.getValue() != null,
                        StrUtil.format("{} @> '{{}}'",
                                columnName,
                                CollUtil.join((Collection) whereCondition.getValue(), ",")));
                break;
            case CONTAINS_ANY:
                wrapper.apply(
                        whereCondition.getValue() != null,
                        StrUtil.format("{} && '{{}}'",
                                columnName,
                                CollUtil.join((Collection) whereCondition.getValue(), ",")));
                break;
            case JSON_OBJECT_PATH_EQUAL:
                if (whereCondition.getValue() == null) {
                    return;
                }
                String[] path = columnName.split("\\.", 2);
                path[1] = formatMysqlJsonPath(path[1]);
                switch (getDbType()) {
                    case "":
                    case "mysql":
                        wrapper.apply(
                                StrUtil.format("{}->'$.{}' = {0}", path[0], path[1]),
                                whereCondition.getValue()
                        );
                        break;
                    case "dm":
                        wrapper.like(whereCondition.getValue() != null, path[0], whereCondition.getValue());
                        break;
                }
                break;
            case JSON_OBJECT_PATH_LIKE:
                if (whereCondition.getValue() == null) {
                    return;
                }
                String[] fieldPath = columnName.split("\\.", 2);
                fieldPath[1] = formatMysqlJsonPath(fieldPath[1]);
                switch (getDbType()) {
                    case "":
                    case "mysql":
                        wrapper.apply(
                                StrUtil.format("{}->'$.{}' like concat('%',{0},'%')", fieldPath[0], fieldPath[1]),
                                whereCondition.getValue()
                        );
                        break;
                    case "dm":
                        wrapper.like(whereCondition.getValue() != null, fieldPath[0], whereCondition.getValue());
                        break;
                }
                break;
            case JSON_ARRAY_CONTAINS:
                if (whereCondition.getValue() == null) {
                    return;
                }
                switch (getDbType()) {
                    case "":
                    case "mysql":
                        try {
                            wrapper.apply(
                                    StrUtil.format("JSON_CONTAINS({},{0})", columnName),
                                    JsonUtil.DEFAULT_INSTANCE.writeValueAsString(Collections.singletonList(whereCondition.getValue()))
                            );
                        } catch (JacksonException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "dm":
                        wrapper.apply(
                                StrUtil.format("{} like concat('%',{0},'%')", columnName),
                                whereCondition.getValue()
                        );
                        break;
                }

                break;
            case JSON_ARRAY_CONTAINS_ANY:
                Collection<?> containsAnyValues = (Collection<?>) whereCondition.getValue();
                if (CollUtil.isEmpty(containsAnyValues)) {
                    return;
                }
                switch (getDbType()) {
                    case "":
                    case "mysql":
                        try {
                            List<String> sqlFragments = new ArrayList<>(containsAnyValues.size());
                            Object[] valueArr = new Object[containsAnyValues.size()];
                            int i = 0;
                            for (Object o : containsAnyValues) {
                                sqlFragments.add(StrUtil.format("JSON_CONTAINS({},{{}})", columnName, i));
                                valueArr[i] = JsonUtil.DEFAULT_INSTANCE.writeValueAsString(o);
                                i++;
                            }
                            String whereSql = CollUtil.join(sqlFragments, " OR ");
                            wrapper.apply(
                                    whereSql,
                                    valueArr
                            );
                        } catch (JacksonException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "dm":
                        List<String> sqlFragments = new ArrayList<>(containsAnyValues.size());
                        Object[] valueArr = new Object[containsAnyValues.size()];
                        int i = 0;
                        for (Object o : containsAnyValues) {
                            sqlFragments.add(StrUtil.format("{} like concat('%',{{}},'%')", columnName, i));
                            valueArr[i] = o;
                            i++;
                        }
                        String whereSql = CollUtil.join(sqlFragments, " OR ");
                        wrapper.apply(
                                whereSql,
                                valueArr
                        );
                        break;
                }

                break;
            case JSON_ARRAY_CONTAINS_ALL:
                Collection<?> containsAllValues = (Collection<?>) whereCondition.getValue();
                if (CollUtil.isEmpty(containsAllValues)) {
                    return;
                }

                switch (getDbType()) {
                    case "":
                    case "mysql":
                        try {
                            List<String> sqlFragments = new ArrayList<>(containsAllValues.size());
                            Object[] valueArr = new Object[containsAllValues.size()];
                            int i = 0;
                            for (Object o : containsAllValues) {
                                sqlFragments.add(StrUtil.format("JSON_CONTAINS({},{{}})", columnName, i));
                                valueArr[i] = JsonUtil.DEFAULT_INSTANCE.writeValueAsString(o);
                                i++;
                            }
                            String whereSql = CollUtil.join(sqlFragments, " AND ");
                            wrapper.apply(
                                    whereSql,
                                    valueArr
                            );
                        } catch (JacksonException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "dm":
                        List<String> sqlFragments = new ArrayList<>(containsAllValues.size());
                        Object[] valueArr = new Object[containsAllValues.size()];
                        int i = 0;
                        for (Object o : containsAllValues) {
                            sqlFragments.add(StrUtil.format("{} like concat('%',{{}},'%')", columnName, i));
                            valueArr[i] = o;
                            i++;
                        }
                        String whereSql = CollUtil.join(sqlFragments, " AND ");
                        wrapper.apply(
                                whereSql,
                                valueArr
                        );
                        break;
                }

                break;
            default:
                throw new UnsupportedOperationException(StrUtil.format("不支持的[{}]条件转换成mybatis plus wrapper", whereCondition.getCondition()));
        }
    }

    @SneakyThrows
    private static String getDbType() {
        DynamicRoutingDataSource ds = BeanProviderUtil.getBean(DynamicRoutingDataSource.class);
        return BeanProviderUtil.getBean(DatabaseIdProvider.class)
                .getDatabaseId(ds);
    }

    private static String formatMysqlJsonPath(String jsonPath) {
        if (jsonPath.contains("-")) {
            if (jsonPath.contains(".")) {
                jsonPath = Arrays.stream(jsonPath.split("\\."))
                        .map(p -> p.contains("-") ? StrUtil.format("\"{}\"", p) : p)
                        .collect(Collectors.joining("."));
            } else {
                jsonPath = StrUtil.format("\"{}\"", jsonPath);
            }
        }
        return jsonPath;
    }
}
