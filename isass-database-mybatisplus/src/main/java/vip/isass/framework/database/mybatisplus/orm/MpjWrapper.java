// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.github.yulichang.config.ConfigProperties;
import com.github.yulichang.extension.apt.AptQueryWrapper;
import com.github.yulichang.extension.apt.matedata.BaseColumn;
import com.github.yulichang.extension.apt.matedata.Column;
import com.github.yulichang.extension.apt.toolkit.AptWrapperUtils;
import com.github.yulichang.wrapper.segments.SelectApt;
import com.github.yulichang.wrapper.segments.SelectCache;

/**
 * Criteria 使用 MPJ 的结构化动态字段；子查询与父查询共享 ORM 参数空间。
 */
final class MpjWrapper<T> extends AptQueryWrapper<T> {

    /**
     * 标识查询 Wrapper 的使用位置，供构建阶段确定选列与关联对象的结果映射。
     */
    enum QueryUsage {

        /** 查询实体及其关联对象；按实体选列规则补齐装配所需的主键。 */
        ENTITY,

        /** FROM 派生表的输入；保留根表全部持久化列供外层筛选和装配。 */
        FROM,

        /** JOIN 派生表的输入；保留根表列，并输出嵌套关联列供外层映射。 */
        JOIN,

        /** EXISTS/NOT_EXISTS 的输入；只选择常量，不配置关联结果映射。 */
        EXISTS,

        /** 供 IN、NOT_IN 子查询使用；要求 Criteria 显式且仅选择一个字段。 */
        SINGLE,

        /** 供关联计数使用；选择根实体字段，但不配置关联对象的结果映射。 */
        COUNT
    }

    /**
     * 根据实体类型创建根查询 Wrapper，每次分配独立的表实例的标识。
     *
     * @param type 根表实体类型
     */
    public MpjWrapper(Class<T> type) {
        this(BaseColumnFactory.create(type));
    }

    /**
     * 使用已有表实例的标识创建独立的查询 Wrapper。
     *
     * @param baseColumn 当前查询的表实例的标识
     */
    private MpjWrapper(BaseColumn<T> baseColumn) {
        super(baseColumn);
    }

    /**
     * 为 JOIN ON 创建子 Wrapper，继承父查询的参数空间和表实例映射。
     *
     * @param parent    父查询 Wrapper
     * @param index     JOIN 表实例序号
     * @param keyword   JOIN 类型关键字
     * @param joinClass JOIN 目标实体类型
     * @param tableName JOIN 目标表名
     */
    private MpjWrapper(MpjWrapper<T> parent, Integer index, String keyword, Class<?> joinClass, String tableName) {
        super(parent.getEntity(), parent.baseColumn, null, parent.paramNameSeq, parent.paramNameValuePairs,
                new MergeSegments(), parent.paramAlias, SharedString.emptyString(), SharedString.emptyString(),
                SharedString.emptyString(), parent.aptIndex, index, keyword, joinClass, tableName, parent.ifExists);
        alias = parent.alias;
    }

    /**
     * 将内层查询已输出的列再次选出，供外层查询使用，并保留该列的 Java 类型和主键标记。
     *
     * @param table       内层查询作为派生表时的表实例标识
     * @param inputAlias  内层查询输出的列别名
     * @param outputAlias 本层查询输出的列别名
     * @param original    内层列的类型和主键等映射信息
     */
    void derivedColumn(BaseColumn<?> table, String inputAlias, String outputAlias, SelectCache original) {
        SelectCache column = new SelectCache(original.getClazz(), original.isPk(), inputAlias, original.getColumnType(),
                original.getColumProperty(), true, null);
        getSelectColumns().add(new SelectApt(column, new Column(table, original.getColumProperty()), outputAlias));
    }

    /**
     * 生成 JOIN SQL；CROSS JOIN 不附加 ON，其余 JOIN 沿用 MPJ 的条件片段。
     *
     * @return 当前查询的 JOIN SQL 片段
     */
    @Override
    public String getFrom() {
        if (onWrappers.stream().noneMatch(join -> "CROSS JOIN".equals(join.getKeyWord()))) {
            return super.getFrom();
        }
        // MPJ 1.5.9 总是追加 ON；这里只扩展 CROSS 的 JOIN 片段，表名/别名/表达式仍来自 MPJ。
        StringBuilder sql = new StringBuilder();
        for (AptQueryWrapper<T> join : onWrappers) {
            sql.append(' ').append(join.getKeyWord()).append(' ').append(join.getTableName()).append(' ').append(join.isHasAlias() ? join.getAlias() : join.getAlias() + join.getIndex());
            if (!"CROSS JOIN".equals(join.getKeyWord())) {
                sql.append(" ON ").append(join.getExpression().getNormal().getSqlSegment());
            }
        }
        return sql.toString();
    }


    /**
     * 创建内层查询 Wrapper，共享参数绑定，并为其分配独立的 SQL 别名作用域。
     *
     * @param type       内层查询的根实体类型
     * @param correlated 是否允许内层条件引用外层表实例
     * @param <R>        内层查询的实体类型
     * @return 尚未编译 Criteria 的内层查询 Wrapper
     */
    <R> MpjWrapper<R> subquery(Class<R> type, boolean correlated) {
        MpjWrapper<R> child = new MpjWrapper<>(type);
        child.paramNameSeq = paramNameSeq;
        child.paramNameValuePairs = paramNameValuePairs;
        child.paramAlias = paramAlias;
        // 同级子查询属于不同 SQL 作用域，可复用别名；嵌套时沿用 MPJ 的子查询别名前缀，避免遮蔽外层。
        child.alias = alias + ConfigProperties.subQueryAlias;
        child.aptIndex.setRootAlias(child.alias);
        if (correlated) {
            child.aptIndex.setParent(aptIndex);
        }
        return child;
    }

    /**
     * 把一个 AptQueryWrapper 渲染成一条 SELECT … FROM … WHERE … SQL
     * 上层调用这个方法后，把这段 SQL 放进 FROM (SELECT …)、JOIN (SELECT …)，或 WHERE EXISTS/IN (SELECT …)
     *
     * @return sql
     */
    String querySql() {
        return AptWrapperUtils.buildUnionSqlByWrapper(getEntityClass(), this);
    }

    /**
     * 实现 FROM (子查询) t
     * <p>
     * MPJ 官方 from 文档有这个功能的示例：MPJLambdaWrapper.from(from -> ...)
     * 会生成 FROM (SELECT ... FROM user ...) t。
     * 但当前框架使用的是 AptQueryWrapper；
     * 我检查了项目依赖的 MPJ 1.5.9 源码，它没有对应的 from(...) 方法。
     * 因此我们实现 MpjWrapper.from()
     * </p>
     *
     * @param input 用作 FROM 派生表的内层查询
     */
    void from(MpjWrapper<?> input) {
        String inputSql = input.querySql();
        setTableName(ignored -> "(" + inputSql + ")");
        disableLogicDel();
    }

    /**
     * 为 MPJ 的 JOIN 条件创建同类子 Wrapper。
     *
     * @param index     JOIN 表实例序号
     * @param keyword   JOIN 类型关键字
     * @param joinClass JOIN 目标实体类型
     * @param tableName JOIN 目标表名
     * @return 与父查询共享上下文的子 Wrapper
     */
    @Override
    protected AptQueryWrapper<T> instance(Integer index, String keyword, Class<?> joinClass, String tableName) {
        return new MpjWrapper<>(this, index, keyword, joinClass, tableName);
    }

    /**
     * 创建保留相同表实例的标识的空 Wrapper，并保持具体 Wrapper 类型。
     *
     * @return 空的同类 Wrapper
     */
    @Override
    protected AptQueryWrapper<T> instanceEmpty() {
        return new MpjWrapper<>(baseColumn);
    }

}
