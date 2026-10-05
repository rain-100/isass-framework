// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.AnalyticExpression;
import net.sf.jsqlparser.expression.AnalyticType;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 对 MPJ 生成并完成参数绑定的 SQL 做 AST 分页改写，不手工组装完整 SQL。
 * 主表未匹配行独立编号，匹配行按根主键共享页序号；展开行不会被 LIMIT 截断。
 *
 * <p>以 BSP 的 Tenant.apps 为例：租户 1 有两个 App，租户 2 有一个 App，按租户每页取 2 条。
 * 普通 JOIN 分页直接截取两行，只会返回租户 1 的两个 App：</p>
 * <pre>{@code
 * SELECT t.id AS root_id, a.id AS app_id
 * FROM bsp_auth_tenant t
 * LEFT JOIN bsp_auth_app a ON a.owner_tenant_id = t.id
 * ORDER BY t.id, a.id
 * LIMIT 0, 2
 * }</pre>
 *
 * <p>本类生成的结构简化如下（实际列别名和过滤条件由 MPJ SQL 决定）。
 * 同一 root_id 的所有 JOIN 行共享 __isass_page_unit_number，因此第一页保留租户 1 的两行和租户 2 的一行；
 * RIGHT/FULL JOIN 中 root_id 为 null 的行则各自占一个页序号：</p>
 * <pre>{@code
 * SELECT * FROM (
 *   SELECT isass_grouped_join_results.*,
 *          DENSE_RANK() OVER (ORDER BY __isass_page_unit_start_position) AS __isass_page_unit_number
 *   FROM (
 *     SELECT isass_ordered_join_results.*,
 *            CASE WHEN root_id IS NULL THEN __isass_join_result_position
 *                 ELSE MIN(__isass_join_result_position) OVER (PARTITION BY root_id)
 *            END AS __isass_page_unit_start_position
 *     FROM (
 *       SELECT t.id AS root_id, a.id AS app_id,
 *              ROW_NUMBER() OVER (ORDER BY t.id, a.id) AS __isass_join_result_position
 *       FROM bsp_auth_tenant t
 *       LEFT JOIN bsp_auth_app a ON a.owner_tenant_id = t.id
 *     ) isass_ordered_join_results
 *   ) isass_grouped_join_results
 * ) isass_numbered_join_results
 * WHERE __isass_page_unit_number > 0 AND __isass_page_unit_number <= 2
 * ORDER BY __isass_page_unit_number, __isass_join_result_position
 * }</pre>
 *
 * <p>同一批示例数据若直接统计 JOIN 结果，下面的 COUNT(*) 得到 3，而不是租户总数 2：</p>
 * <pre>{@code
 * SELECT COUNT(*)
 * FROM bsp_auth_tenant t
 * LEFT JOIN bsp_auth_app a ON a.owner_tenant_id = t.id
 * }</pre>
 *
 * <p>本类将 COUNT 改写为以下结构：非空根主键按不同租户计数，空根主键的 JOIN 结果逐行计数。
 * 该 LEFT JOIN 示例得到 2；实际内部投影与筛选条件仍由 MPJ SQL 决定：</p>
 * <pre>{@code
 * SELECT COUNT(DISTINCT root_id) + (COUNT(*) - COUNT(root_id)) AS total
 * FROM (
 *   SELECT t.id AS root_id, a.id AS app_id
 *   FROM bsp_auth_tenant t
 *   LEFT JOIN bsp_auth_app a ON a.owner_tenant_id = t.id
 * ) isass_count_source
 * }</pre>
 */
final class AdvJoinPageSql {
    private static final String JOIN_RESULT_POSITION = "__isass_join_result_position";
    private static final String PAGE_UNIT_START_POSITION = "__isass_page_unit_start_position";
    private static final String PAGE_UNIT_NUMBER = "__isass_page_unit_number";

    private AdvJoinPageSql() {
    }

    static String count(String sql, String keyAlias) {
        PlainSelect input = parse(sql);
        input.setOrderByElements(null);
        Function roots = function("COUNT", new Column(keyAlias));
        roots.setDistinct(true);
        Subtraction unmatched = new Subtraction();
        unmatched.setLeftExpression(function("COUNT", new AllColumns()));
        unmatched.setRightExpression(function("COUNT", new Column(keyAlias)));
        Addition total = new Addition();
        total.setLeftExpression(roots);
        total.setRightExpression(unmatched);
        PlainSelect result = from(input, "isass_count_source");
        result.setSelectItems(List.of(SelectItem.from(total)));
        return result.toString();
    }

    static String page(String sql, String keyAlias, List<String> identityAliases, long offset, long size) {
        PlainSelect input = parse(sql);
        Expression rootKey = input.getSelectItems().stream()
                .filter(item -> item.getAlias() != null && keyAlias.equals(item.getAlias().getName()))
                .findFirst().orElseThrow(() -> new IllegalStateException("JOIN 分页缺少内部根主键投影"))
                .getExpression();
        List<OrderByElement> order = input.getOrderByElements() == null
                ? new ArrayList<>() : new ArrayList<>(input.getOrderByElements());
        order.add(order(rootKey));
        // 空根行的根 ID 都是 null；用各关联主键破除并列，避免翻页时随机重复/漏行。
        for (SelectItem<?> item : input.getSelectItems()) {
            if (item.getAlias() != null && identityAliases.contains(item.getAlias().getName())
                    && !keyAlias.equals(item.getAlias().getName())) {
                order.add(order(item.getExpression()));
            }
        }
        AnalyticExpression joinResultPosition = analytic("ROW_NUMBER", null);
        joinResultPosition.setOrderByElements(order);
        input.addSelectItem(joinResultPosition, new Alias(JOIN_RESULT_POSITION));
        input.setOrderByElements(null);

        AnalyticExpression firstJoinResultPosition = analytic("MIN", new Column(JOIN_RESULT_POSITION));
        firstJoinResultPosition.setPartitionExpressionList(new ExpressionList<>(new Column(keyAlias)));
        WhenClause unmatchedRoot = new WhenClause();
        unmatchedRoot.setWhenExpression(new IsNullExpression().withLeftExpression(new Column(keyAlias)));
        unmatchedRoot.setThenExpression(new Column(JOIN_RESULT_POSITION));
        CaseExpression pageUnitStartPosition = new CaseExpression();
        pageUnitStartPosition.setWhenClauses(List.of(unmatchedRoot));
        pageUnitStartPosition.setElseExpression(firstJoinResultPosition);
        PlainSelect groupedJoinResults = from(input, "isass_ordered_join_results");
        groupedJoinResults.addSelectItems(new AllColumns());
        groupedJoinResults.addSelectItem(pageUnitStartPosition, new Alias(PAGE_UNIT_START_POSITION));

        AnalyticExpression pageUnitNumber = analytic("DENSE_RANK", null);
        pageUnitNumber.setOrderByElements(List.of(order(new Column(PAGE_UNIT_START_POSITION))));
        PlainSelect numberedJoinResults = from(groupedJoinResults, "isass_grouped_join_results");
        numberedJoinResults.addSelectItems(new AllColumns());
        numberedJoinResults.addSelectItem(pageUnitNumber, new Alias(PAGE_UNIT_NUMBER));

        GreaterThan start = new GreaterThan();
        start.setLeftExpression(new Column(PAGE_UNIT_NUMBER));
        start.setRightExpression(new LongValue(offset));
        MinorThanEquals end = new MinorThanEquals();
        end.setLeftExpression(new Column(PAGE_UNIT_NUMBER));
        end.setRightExpression(new LongValue(Math.addExact(offset, size)));
        PlainSelect result = from(numberedJoinResults, "isass_numbered_join_results");
        result.addSelectItems(new AllColumns());
        result.setWhere(new AndExpression(start, end));
        result.setOrderByElements(List.of(order(new Column(PAGE_UNIT_NUMBER)),
                order(new Column(JOIN_RESULT_POSITION))));
        return result.toString();
    }

    private static PlainSelect parse(String sql) {
        try {
            if (JsqlParserGlobal.parse(sql) instanceof PlainSelect select) {
                return select;
            }
            throw new IllegalArgumentException("JOIN 分页仅支持结构化 SELECT");
        } catch (JSQLParserException exception) {
            throw new IllegalArgumentException("无法解析 ORM 生成的 JOIN SQL", exception);
        }
    }

    private static PlainSelect from(PlainSelect source, String alias) {
        return new PlainSelect().withFromItem(new ParenthesedSelect().withSelect(source).withAlias(new Alias(alias)));
    }

    private static OrderByElement order(Expression expression) {
        return new OrderByElement().withExpression(expression);
    }

    private static Function function(String name, Expression argument) {
        return new Function().withName(name).withParameters(new ExpressionList<>(argument));
    }

    private static AnalyticExpression analytic(String name, Expression argument) {
        return new AnalyticExpression().withName(name).withExpression(argument).withType(AnalyticType.OVER);
    }
}
