// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ParameterUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.yulichang.wrapper.segments.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 普通查询保持 MP 分页；Criteria JOIN 使用根实体/空根行分页语义。
 */
public final class AdvJoinPaginationInnerInterceptor extends PaginationInnerInterceptor {

    @Override
    public boolean willDoQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds bounds,
                               ResultHandler handler, BoundSql sql) throws SQLException {
        MpjWrapper<?> wrapper = joinedWrapper(parameter);
        IPage<?> page = ParameterUtils.findPage(parameter).orElse(null);
        if (wrapper == null || page == null) {
            return super.willDoQuery(executor, statement, parameter, bounds, handler, sql);
        }
        if (page.searchCount()) {
            MappedStatement countStatement = buildAutoCountMappedStatement(statement);
            String countQuery = AdvJoinPageSql.count(sql.getSql(), rootKeyAlias(wrapper));
            BoundSql countSql = new BoundSql(statement.getConfiguration(), countQuery, sql.getParameterMappings(), parameter);
            PluginUtils.setAdditionalParameter(countSql, PluginUtils.mpBoundSql(sql).additionalParameters());
            List<Object> count = executor.query(countStatement, parameter, bounds, handler,
                    executor.createCacheKey(countStatement, parameter, bounds, countSql), countSql);
            page.setTotal(count.isEmpty() ? 0 : ((Number) count.getFirst()).longValue());
            if (page.getTotal() == 0) {
                return false;
            }
        }
        return page.getSize() != 0;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement statement, Object parameter, RowBounds bounds,
                            ResultHandler handler, BoundSql sql) {
        MpjWrapper<?> wrapper = joinedWrapper(parameter);
        IPage<?> page = ParameterUtils.findPage(parameter).orElse(null);
        if (wrapper == null || page == null) {
            super.beforeQuery(executor, statement, parameter, bounds, handler, sql);
            return;
        }
        handlerLimit(page, page.maxLimit() == null ? getMaxLimit() : page.maxLimit());
        List<String> identities = wrapper.getSelectColumns().stream()
                .filter(Select::isPk).map(AdvJoinPaginationInnerInterceptor::resultAlias).distinct().toList();
        PluginUtils.mpBoundSql(sql).sql(AdvJoinPageSql.page(sql.getSql(), rootKeyAlias(wrapper),
                identities, page.offset(), page.getSize()));
    }

    private static String rootKeyAlias(MpjWrapper<?> wrapper) {
        return wrapper.getSelectColumns().stream()
                .filter(select -> select.isPk() && !select.isLabel()
                        && select.getClazz() == wrapper.getEntityClass())
                .findFirst().map(AdvJoinPaginationInnerInterceptor::resultAlias)
                .orElseThrow(() -> new IllegalArgumentException("JOIN 分页缺少根实体主键投影"));
    }

    private static String resultAlias(Select select) {
        return select.isHasAlias() ? select.getAlias() : select.getTagColumn();
    }

    private static MpjWrapper<?> joinedWrapper(Object parameter) {
        if (parameter instanceof Map<?, ?> values && values.get("ew") instanceof MpjWrapper<?> wrapper
                && !wrapper.getFrom().isBlank()) {
            return wrapper;
        }
        return null;
    }
}
