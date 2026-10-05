import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import com.github.yulichang.base.MPJBaseMapper;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.database.mybatisplus.config.SqlSessionConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Diagnostic only: uses a disposable H2 database, never any configured application data source. */
public class IsassMpjDatabaseProbe {
    @Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
    public static class SqlTrace implements Interceptor {
        @Override public Object intercept(Invocation invocation) throws Throwable {
            System.out.println("SQL " + ((StatementHandler) invocation.getTarget()).getBoundSql().getSql()
                    .replaceAll("\\s+", " "));
            return invocation.proceed();
        }
    }
    @TableName("probe_parent")
    public static class Parent {
        @TableId public Long id;
        public Long tenantId;
        public String name;
        @TableLogic(value = "false", delval = "true") public Boolean deleted;
        @Version public Integer version;
        @TableField(exist = false) public List<Child> children;
        @TableField(exist = false) public Child child;
        public Long getId() { return id; }
        public Long getTenantId() { return tenantId; }
        public String getName() { return name; }
        public Boolean getDeleted() { return deleted; }
        public List<Child> getChildren() { return children; }
        public Child getChild() { return child; }
    }

    @TableName("probe_child")
    public static class Child {
        @TableId public Long id;
        public Long parentId;
        public Long tenantId;
        public String name;
        @TableLogic(value = "false", delval = "true") public Boolean deleted;
        public Long getId() { return id; }
        public Long getParentId() { return parentId; }
        public Long getTenantId() { return tenantId; }
        public String getName() { return name; }
        public Boolean getDeleted() { return deleted; }
    }

    public interface ParentMapper extends MPJBaseMapper<Parent> {}
    public interface ChildMapper extends MPJBaseMapper<Child> {}

    static int passed;
    static final List<String> findings = new ArrayList<>();

    static void check(String name, Runnable action) {
        try {
            action.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable error) {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            String message = name + " => " + cause.getClass().getSimpleName() + ": " + cause.getMessage();
            findings.add(message);
            System.out.println("FINDING " + message.replaceAll("\\s+", " "));
        }
    }

    static void expect(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }

    static MPJLambdaWrapper<Parent> left() {
        return new MPJLambdaWrapper<>(Parent.class)
                .selectAll(Parent.class).selectCollection(Child.class, Parent::getChildren)
                .leftJoin(Child.class, on -> on.eq(Child::getParentId, Parent::getId).eq(Child::getTenantId, 1L))
                .eq(Parent::getTenantId, 1L).orderByAsc(Parent::getId).orderByAsc(Child::getId);
    }

    static MPJLambdaWrapper<Parent> right(boolean disableAutomaticLogic) {
        var wrapper = new MPJLambdaWrapper<>(Parent.class)
                .selectAll(Parent.class).selectCollection(Child.class, Parent::getChildren)
                .rightJoin(Child.class, Child::getParentId, Parent::getId)
                .eq(Child::getTenantId, 1L).eq(Child::getDeleted, false)
                .orderByAsc(Child::getId);
        if (disableAutomaticLogic) wrapper.disableLogicDel().disableSubLogicDel();
        return wrapper;
    }

    static MPJLambdaWrapper<Parent> mainPage() {
        return new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                .selectCollection(Child.class, Parent::getChildren)
                .leftJoin(Child.class, on -> on.eq(Child::getParentId, Parent::getId)
                        .eq(Child::getTenantId, 1L))
                .eq(Parent::getTenantId, 1L).orderByAsc(Parent::getId).pageByMain();
    }

    static MPJLambdaWrapper<Parent> scopedRight() {
        return new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                .selectCollection(Child.class, Parent::getChildren)
                .from(root -> root.selectAll().eq(Parent::getTenantId, 1L))
                .rightJoin(Child.class, child -> child.selectAll().eq(Child::getTenantId, 1L),
                        Child::getParentId, Parent::getId)
                .disableLogicDel().disableSubLogicDel().orderByAsc(Child::getId);
    }

    static void runCases(SqlSessionFactory factory) {
        factory.getConfiguration().addMapper(ParentMapper.class);
        factory.getConfiguration().addMapper(ChildMapper.class);
        System.out.println("PLUGINS " + factory.getConfiguration().getInterceptors().stream()
                .map(value -> value.getClass().getSimpleName()).toList());
        try (SqlSession session = factory.openSession(true)) {
            ParentMapper mapper = session.getMapper(ParentMapper.class);

            check("base mapper logical deletion", () -> expect(List.of(1L, 2L, 3L),
                    mapper.selectList(Wrappers.<Parent>lambdaQuery().orderByAsc(Parent::getId))
                            .stream().map(Parent::getId).toList()));

            check("LEFT JOIN collection, empty relation and explicit tenant scope", () -> {
                var wrapper = left();
                var rows = mapper.selectJoinList(Parent.class, wrapper);
                expect(List.of(1L, 2L), rows.stream().map(Parent::getId).toList());
                expect(List.of(11L, 12L), rows.getFirst().children.stream().map(Child::getId).toList());
                expect(0, rows.get(1).children.size());
            });

            check("INNER JOIN collection", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .selectCollection(Child.class, Parent::getChildren)
                        .innerJoin(Child.class, on -> on.eq(Child::getParentId, Parent::getId)
                                .eq(Child::getTenantId, 1L))
                        .eq(Parent::getTenantId, 1L);
                var rows = mapper.selectJoinList(Parent.class, wrapper);
                expect(1, rows.size());
                expect(2, rows.getFirst().children.size());
            });

            check("ordinary JOIN page must contain complete root collection", () -> {
                var result = mapper.selectJoinPage(new Page<Parent>(1, 1), Parent.class, left());
                System.out.println("PAGE ordinary total=" + result.getTotal() + ", roots="
                        + result.getRecords().size() + ", children=" + result.getRecords().getFirst().children.size());
                expect(2, result.getRecords().getFirst().children.size());
                expect(2L, result.getTotal());
            });

            check("pageByMain first page and root count", () -> {
                // Child ordering is not part of the root-only pagination plan in this case.
                var wrapper = mainPage();
                var result = mapper.selectJoinPage(new Page<Parent>(1, 1), Parent.class, wrapper);
                System.out.println("PAGE main total=" + result.getTotal() + ", children="
                        + result.getRecords().getFirst().children.size());
                expect(2L, result.getTotal());
                expect(2, result.getRecords().getFirst().children.size());
            });

            check("pageByMain second page with NEW wrapper", () -> {
                var second = mapper.selectJoinPage(new Page<Parent>(2, 1), Parent.class, mainPage());
                expect(2L, second.getRecords().getFirst().id);
                expect(0, second.getRecords().getFirst().children.size());
            });

            check("derived FROM and derived JOIN target", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .selectCollection(Child.class, Parent::getChildren)
                        .from(root -> root.selectAll().eq(Parent::getTenantId, 1L))
                        .leftJoin(Child.class, child -> child.selectAll().eq(Child::getTenantId, 1L)
                                        .eq(Child::getName, "alpha"), Child::getParentId, Parent::getId)
                        .orderByAsc(Parent::getId);
                var rows = mapper.selectJoinList(Parent.class, wrapper);
                expect(2, rows.size());
                expect(List.of(11L), rows.getFirst().children.stream().map(Child::getId).toList());
            });

            check("correlated EXISTS", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .eq(Parent::getTenantId, 1L)
                        .exists(Child.class, child -> child.select(Child::getId)
                                .eq(Child::getParentId, Parent::getId).eq(Child::getTenantId, 1L));
                expect(List.of(1L), mapper.selectJoinList(Parent.class, wrapper).stream().map(Parent::getId).toList());
            });

            check("NOT EXISTS and IN/NOT IN subqueries", () -> {
                var absent = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .eq(Parent::getTenantId, 1L).notExists(Child.class, child -> child.select(Child::getId)
                                .eq(Child::getParentId, Parent::getId).eq(Child::getTenantId, 1L));
                expect(List.of(2L), mapper.selectJoinList(Parent.class, absent).stream().map(Parent::getId).toList());
                var present = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .eq(Parent::getTenantId, 1L).in(Parent::getId, Child.class,
                                child -> child.select(Child::getParentId).eq(Child::getTenantId, 1L));
                expect(List.of(1L), mapper.selectJoinList(Parent.class, present).stream().map(Parent::getId).toList());
                var notIn = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .eq(Parent::getTenantId, 1L).notIn(Parent::getId, Child.class,
                                child -> child.select(Child::getParentId).eq(Child::getTenantId, 1L));
                expect(List.of(2L), mapper.selectJoinList(Parent.class, notIn).stream().map(Parent::getId).toList());
            });

            check("RIGHT JOIN default logic keeps unmatched children", () -> {
                var wrapper = right(false);
                var rows = mapper.selectJoinList(Parent.class, wrapper);
                long unmatched = rows.stream().filter(row -> row.id == null).count();
                expect(2L, unmatched);
            });

            check("RIGHT JOIN null-root assembly without automatic logic", () -> {
                var rows = mapper.selectJoinList(Parent.class, right(true));
                System.out.println("RIGHT roots=" + rows.stream().map(Parent::getId).toList());
                var unmatched = rows.stream().filter(row -> row.id == null).toList();
                expect(2, unmatched.size());
                expect(false, unmatched.get(0) == unmatched.get(1));
                expect(List.of(91L, 92L), unmatched.stream().map(row -> row.children.getFirst().id).toList());
            });

            check("RIGHT pageByMain null-root count and preservation", () -> {
                var result = mapper.selectJoinPage(new Page<Parent>(1, 20), Parent.class, right(true).pageByMain());
                System.out.println("RIGHT PAGE total=" + result.getTotal() + ", roots="
                        + result.getRecords().stream().map(Parent::getId).toList());
                expect(3L, result.getTotal());
                expect(2L, result.getRecords().stream().filter(row -> row.id == null).count());
            });

            check("RIGHT input-side logical deletion and explicit tenant scope", () -> {
                var rows = mapper.selectJoinList(Parent.class, scopedRight());
                expect(3, rows.size());
                expect(2L, rows.stream().filter(row -> row.id == null).count());
                expect(List.of(11L, 12L, 91L, 92L), rows.stream().flatMap(row -> row.children.stream())
                        .map(Child::getId).sorted().toList());
            });

            check("RIGHT pageByMain with input-side scopes counts entity units", () -> {
                var wrapper = scopedRight().pageByMain();
                wrapper.getExpression().getOrderBy().clear();
                var result = mapper.selectJoinPage(new Page<Parent>(1, 20), Parent.class, wrapper);
                System.out.println("RIGHT SCOPED PAGE total=" + result.getTotal() + ", roots="
                        + result.getRecords().stream().map(Parent::getId).toList());
                expect(3L, result.getTotal());
            });

            check("root-only tenant scope negative control", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .selectCollection(Child.class, Parent::getChildren)
                        .leftJoin(Child.class, Child::getParentId, Parent::getId)
                        .eq(Parent::getTenantId, 1L).eq(Parent::getId, 1L);
                var ids = mapper.selectJoinList(Parent.class, wrapper).getFirst().children.stream()
                        .map(Child::getId).sorted().toList();
                System.out.println("NEGATIVE CONTROL root-only scope children=" + ids);
                // This demonstrates missing scope propagation, not successful tenant isolation.
                expect(List.of(11L, 12L, 14L), ids);
            });

            check("single association rejects multiple distinct targets", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .selectAssociation(Child.class, Parent::getChild)
                        .leftJoin(Child.class, on -> on.eq(Child::getParentId, Parent::getId)
                                .eq(Child::getTenantId, 1L)).eq(Parent::getId, 1L);
                var rows = mapper.selectJoinList(Parent.class, wrapper);
                throw new AssertionError("No cardinality exception, roots=" + rows.size()
                        + ", selected children=" + rows.stream().map(row -> row.child == null ? null : row.child.id).toList());
            });

            check("CROSS structured SQL execution", () -> {
                var wrapper = new MPJLambdaWrapper<>(Parent.class).selectAll(Parent.class)
                        .disableLogicDel().disableSubLogicDel().join("CROSS JOIN", Child.class, on -> on);
                expect(32, mapper.selectJoinList(Parent.class, wrapper).size());
            });

            check("optimistic locking remains active", () -> {
                Parent first = mapper.selectById(1L);
                Parent stale = mapper.selectById(1L);
                first.name = "updated";
                stale.name = "stale";
                expect(1, mapper.updateById(first));
                expect(0, mapper.updateById(stale));
                expect("updated", mapper.selectById(1L).name);
            });
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("VERSION Boot=" + SpringBootVersion.getVersion()
                + ", MP artifact=" + Path.of(Wrappers.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getFileName()
                + ", MPJ=" + MPJLambdaWrapper.class.getPackage().getImplementationVersion());
        boolean mysqlDialect = args.length > 0 && args[0].equals("mysql-dialect");
        System.out.println("DATABASE H2 2.4.240; dialect=" + (mysqlDialect ? "MySQL (H2 compatibility mode, not MySQL server)" : "H2"));
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:mpj_probe_" + UUID.randomUUID().toString().replace("-", "")
                + (mysqlDialect ? ";MODE=MySQL" : ""));
        try (Connection keeper = dataSource.getConnection(); var statement = keeper.createStatement()) {
            statement.execute("CREATE TABLE probe_parent(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(40), deleted BOOLEAN, version INT)");
            statement.execute("CREATE TABLE probe_child(id BIGINT PRIMARY KEY, parent_id BIGINT, tenant_id BIGINT, name VARCHAR(40), deleted BOOLEAN)");
            statement.execute("INSERT INTO probe_parent VALUES (1,1,'one',false,0),(2,1,'empty',false,0),(3,2,'hidden',false,0),(4,1,'deleted',true,0)");
            statement.execute("INSERT INTO probe_child VALUES (11,1,1,'alpha',false),(12,1,1,'beta',false),(13,1,1,'deleted',true),(14,1,2,'wrong-tenant',false),(31,3,2,'hidden',false),(91,999,1,'orphan-a',false),(92,999,1,'orphan-b',false),(41,4,1,'deleted-parent',true)");
            new ApplicationContextRunner()
                    .withBean(DataSource.class, () -> dataSource)
                    .withBean(SqlTrace.class, SqlTrace::new)
                    .withUserConfiguration(SqlSessionConfig.class)
                    .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class,
                            MybatisPlusJoinAutoConfiguration.class))
                    .withPropertyValues("mybatis-plus-join.banner=false", "mybatis-plus.global-config.banner=false")
                    .run(context -> {
                        if (context.getStartupFailure() != null) {
                            throw new AssertionError("Boot configuration failed", context.getStartupFailure());
                        }
                        if (mysqlDialect) {
                            context.getBean(MybatisPlusInterceptor.class).getInterceptors().stream()
                                    .filter(PaginationInnerInterceptor.class::isInstance)
                                    .map(PaginationInnerInterceptor.class::cast)
                                    .forEach(plugin -> plugin.setDbType(DbType.MYSQL));
                        }
                        runCases(context.getBean(SqlSessionFactory.class));
                    });
        }
        System.out.println("SUMMARY passed=" + passed + ", findings=" + findings.size());
    }
}
