// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus.orm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import com.github.yulichang.base.MPJBaseMapper;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vip.isass.framework.database.mybatisplus.DatabaseMybatisPlusAutoConfiguration;
import vip.isass.framework.common.criteria.JoinCondition;
import vip.isass.framework.common.criteria.NullValueMode;
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.EmptyCriteria;
import vip.isass.framework.common.criteria.impl.type.Condition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IIdEntity;

import javax.sql.DataSource;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真正经过 Repository、MPJ/MyBatis 插件和 JDBC 的隔离回归；从不连接业务数据源。
 */
class MpjCriteriaIntegrationTest {
    @Test
    void executesCriteriaAgainstAnIsolatedDatabase() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:criteria_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class, MybatisPlusJoinAutoConfiguration.class))
                .withUserConfiguration(DatabaseMybatisPlusAutoConfiguration.class)
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("CREATE TABLE criteria_root (id BIGINT PRIMARY KEY, name VARCHAR(80), deleted BOOLEAN)");
                        statement.execute("CREATE TABLE criteria_child (id BIGINT PRIMARY KEY, parent_id BIGINT, name VARCHAR(80), deleted BOOLEAN)");
                        statement.execute("INSERT INTO criteria_root VALUES (1,'first',false),(2,'second',false),(3,'third',false),(4,'deleted',true)");
                        statement.execute("INSERT INTO criteria_child VALUES (11,1,'a',false),(12,1,'b',false),(13,2,'c',false),"
                                + "(14,99,'orphan-a',false),(15,99,'orphan-b',false),(16,1,'deleted',true),(17,4,'deleted-root',false)");
                    }
                    SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
                    factory.getConfiguration().addMapper(RootMapper.class);
                    factory.getConfiguration().addMapper(ChildMapper.class);
                    factory.getConfiguration().addMapper(SetRootMapper.class);
                    try (var session = factory.openSession(true)) {
                        RootRepository repository = new RootRepository(session.getMapper(RootMapper.class));
                        SetRootRepository setRepository = new SetRootRepository(session.getMapper(SetRootMapper.class));
                        // 单表和关联查询走相同的 Wrapper 构建入口；单表仍保留正常投影/分页/逻辑删除。
                        assertEquals(3, repository.countByCriteria(new RootCriteria().setReturnField("name")));
                        MpjWrapper<Root> projectedWrapper = (MpjWrapper<Root>) WrapperUtil.getQueryWrapper(
                                new RootCriteria().setReturnField("name"));
                        String projection = projectedWrapper.getSqlSelect();
                        assertEquals(3, repository.countByWrapper(projectedWrapper));
                        assertEquals(projection, projectedWrapper.getSqlSelect());
                        assertEquals(Set.of("first", "second", "third"), repository.findByWrapper(projectedWrapper).stream()
                                .map(Root::getName).collect(Collectors.toSet()));
                        assertTrue(repository.isPresentByCriteria(new RootCriteria().equals("id", 1L)));
                        assertFalse(repository.isPresentByCriteria(new RootCriteria().equals("id", 4L)));
                        var plainPage = repository.findPageByCriteria(new RootCriteria().setOrderBy("id")
                                .setPageNum(2L).setPageSize(1L));
                        assertEquals(3L, plainPage.getTotal());
                        assertEquals(2L, plainPage.getRecords().getFirst().getId());
                        var projected = repository.findByCriteria(new RootCriteria().equals("id", 1L)
                                .setReturnField("name")).getFirst();
                        assertEquals("first", projected.getName());
                        assertNull(projected.getId());
                        // H2 的 MySQL 模式不实现 MySQL UPDATE JOIN；SQL 由 MPJ 生成，隔离库只检验显式失败。
                        assertThrows(RuntimeException.class, () -> repository.deleteCountByCriteria(new RootCriteria()
                                .leftJoin(Child.class, Root::getId, Child::getParentId).equals("id", -1L)));
                        assertEquals(0, repository.deleteCountByCriteria(new RootCriteria()
                                .exists(new ChildCriteria().equals("id", -1L))));
                        assertEquals(0, repository.deleteCountByCriteria(new RootCriteria()
                                .in(Root::getId, new ChildCriteria().equals("id", -1L), Child::getParentId)));
                        assertEquals(0, repository.deleteCountByCriteria(new RootCriteria().equals("id", -1L)
                                .notIn(Root::getId, new ChildCriteria(), Child::getParentId)));
                        Root noMatch = new Root();
                        noMatch.setName("unchanged");
                        assertEquals(0, repository.updateCountByCriteria(noMatch, new RootCriteria()
                                .exists(new ChildCriteria().equals("id", -1L))));
                        assertThrows(RuntimeException.class, () -> repository.updateCountByCriteria(noMatch, new RootCriteria()
                                .leftJoin(Child.class, Root::getId, Child::getParentId).equals("id", -1L)));
                        assertEquals(0, repository.deleteCountByCriteria(new RootCriteria().equals("id", -1L)));
                        assertEquals(List.of(1L, 3L), repository.findByCriteria(new RootCriteria()
                                .setWhereConditions(List.of(WhereCondition.or(WhereCondition.eq(Root::getName, "first"),
                                        WhereCondition.and(WhereCondition.eq(Root::getName, "third"),
                                                WhereCondition.not(WhereCondition.eq(Root::getId, 2L))))))
                                .setOrderBy("id")).stream().map(Root::getId).toList());
                        assertEquals(List.of(1L, 2L), repository.findByCriteria(new RootCriteria()
                                .exists(new ChildCriteria(), Root::getId, Child::getParentId).setOrderBy("id"))
                                .stream().map(Root::getId).toList());
                        assertEquals(List.of(3L), repository.findByCriteria(new RootCriteria()
                                .notExists(new ChildCriteria(), Root::getId, Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertEquals(List.of(1L), repository.findByCriteria(new RootCriteria().exists(
                                new ChildCriteria().equals("name", "a").orEquals("name", "b"), Root::getId, Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertEquals(List.of(1L), repository.findByCriteria(new RootCriteria().exists(
                                new ChildCriteria().exists(new RootCriteria().equals("name", "first"),
                                        Child::getParentId, Root::getId), Root::getId, Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertEquals(List.of(1L), repository.findByCriteria(new RootCriteria()
                                .exists(new ChildCriteria().equals("name", "a"), Root::getId, Child::getParentId)
                                .exists(new ChildCriteria().equals("name", "b"), Root::getId, Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertEquals(List.of(2L), repository.findByCriteria(new RootCriteria()
                                .in(Root::getId, new ChildCriteria().equals("name", "c"), Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertTrue(repository.isPresentByCriteria(new RootCriteria().exists(new ChildCriteria())));
                        assertFalse(repository.isPresentByCriteria(new RootCriteria().exists(new ChildCriteria().equals("id", -1L))));
                        assertEquals(2, repository.countByCriteria(new RootCriteria()
                                .exists(new ChildCriteria(), Root::getId, Child::getParentId)));
                        var left = repository.findByCriteria(new RootCriteria()
                                .leftJoin(Child.class, Root::getId, Child::getParentId).setOrderBy("id"));
                        assertEquals(List.of(1L, 2L, 3L), left.stream().map(Root::getId).toList());
                        assertEquals(2, left.getFirst().children.size());
                        var setJoin = setRepository.findByCriteria(new SetRootCriteria()
                                .leftJoin(Child.class, SetRoot::getId, Child::getParentId));
                        assertEquals(Set.of(11L, 12L), setJoin.getFirst().childSet.stream().map(Child::getId)
                                .collect(Collectors.toSet()));
                        ChildCriteria selectedChild = new ChildCriteria().setReturnField("name");
                        RootCriteria selectedRoot = new RootCriteria().setReturnField("name")
                                .leftJoin(selectedChild, Root::getId, Child::getParentId).setOrderBy("id");
                        var selectedJoin = repository.findByCriteria(selectedRoot);
                        assertEquals(List.of(1L, 2L, 3L), selectedJoin.stream().map(Root::getId).toList());
                        assertEquals(List.of(11L, 12L), selectedJoin.getFirst().children.stream().map(Child::getId).toList());
                        assertTrue(selectedRoot.getReturnFields().contains("id"));
                        assertEquals(List.of("name"), selectedChild.getReturnFields());
                        var sameProjection = repository.findByCriteria(new RootCriteria()
                                .setReturnField("deleted")
                                .leftJoin(Child.class, Root::getId, Child::getParentId));
                        assertEquals(Set.of(1L, 2L, 3L), sameProjection.stream().map(Root::getId)
                                .collect(Collectors.toSet()));
                        var onFiltered = repository.findByCriteria(new RootCriteria().leftJoin(new JoinCondition()
                                .setTargetCriteria(EmptyCriteria.of(Child.class)).setResultProperty("children")
                                .setChildren(List.of(
                                        WhereCondition.eq(Root::getId, Child::getParentId),
                                        new WhereCondition().setTargetProperty("name").setCondition(Condition.Compare.EQUAL).setValue("a"))))
                                .setOrderBy("id"));
                        assertEquals(List.of(11L), onFiltered.getFirst().children.stream().map(Child::getId).toList());
                        assertEquals(0, onFiltered.get(1).children.size());
                        assertEquals(List.of(3L), repository.findByCriteria(new RootCriteria()
                                .notIn(Root::getId, new ChildCriteria(), Child::getParentId))
                                .stream().map(Root::getId).toList());
                        assertThrows(IllegalArgumentException.class, () -> repository.findByCriteria(new RootCriteria()
                                .leftJoin(new ChildCriteria().setPageSize(1L), Root::getId, Child::getParentId)));
                        var fromRight = repository.findByCriteria(new RootCriteria()
                                .setFromCriteria(new RootCriteria().equals("id", 1L))
                                .rightJoin(Child.class, Root::getId, Child::getParentId));
                        assertEquals(4, fromRight.stream().filter(root -> root.id == null).count());
                        assertEquals(0, left.getLast().children.size());
                        var nested = repository.findByCriteria(new RootCriteria()
                                .leftJoin(new ChildCriteria().leftJoin(Root.class, Child::getParentId, Root::getId),
                                        Root::getId, Child::getParentId).setOrderBy("id"));
                        assertEquals("first", nested.getFirst().children.getFirst().root.name);
                        assertEquals(2, nested.getFirst().children.size());
                        var deeper = repository.findByCriteria(new RootCriteria()
                                .leftJoin(new ChildCriteria().leftJoin(new RootCriteria()
                                                .leftJoin(Child.class, Root::getId, Child::getParentId),
                                        Child::getParentId, Root::getId), Root::getId, Child::getParentId)
                                .setOrderBy("id"));
                        assertEquals(2, deeper.getFirst().children.getFirst().root.children.size());
                        var filteredNested = repository.findByCriteria(new RootCriteria()
                                .leftJoin(new ChildCriteria().equals("name", "a")
                                                .leftJoin(Root.class, Child::getParentId, Root::getId),
                                        Root::getId, Child::getParentId));
                        assertEquals(List.of(11L), filteredNested.stream()
                                .filter(root -> root.id == 1L).findFirst().orElseThrow()
                                .children.stream().map(Child::getId).toList());
                        RootCriteria rootInput = new RootCriteria().setReturnField("name")
                                .in(Root::getId, new ChildCriteria().exists(
                                                new RootCriteria().equals("name", "first"), Child::getParentId, Root::getId),
                                        Child::getParentId);
                        ChildCriteria childInput = new ChildCriteria().setWhereConditions(List.of(
                                new WhereCondition().setCondition(Condition.Logical.OR).setChildren(List.of(
                                        WhereCondition.eq("name", "a"), WhereCondition.eq("name", "b")))));
                        RootCriteria combinedCriteria = new RootCriteria().setFromCriteria(rootInput)
                                .leftJoin(new ChildCriteria().setFromCriteria(childInput)
                                                .leftJoin(new RootCriteria().equals("name", "first"), Child::getParentId, Root::getId),
                                        Root::getId, Child::getParentId)
                                .setReturnField("name").setOrderBy("id DESC").setPageNum(1L).setPageSize(1L);
                        var combinedPage = repository.findPageByCriteria(combinedCriteria);
                        assertEquals(1L, combinedPage.getTotal());
                        assertEquals(1L, combinedPage.getRecords().getFirst().id);
                        assertEquals(Set.of(11L, 12L), combinedPage.getRecords().getFirst().children.stream()
                                .map(Child::getId).collect(Collectors.toSet()));
                        assertTrue(combinedPage.getRecords().getFirst().children.stream()
                                .allMatch(child -> "first".equals(child.root.name)));
                        assertEquals(List.of("name"), rootInput.getReturnFields());
                        assertTrue(childInput.getReturnFields().isEmpty());
                        assertEquals(2, repository.countByCriteria(new RootCriteria().exists(
                                new ChildCriteria().innerJoin(Root.class, Child::getParentId, Root::getId),
                                Root::getId, Child::getParentId)));
                        for (long number = 1; number <= 3; number++) {
                            var page = repository.findPageByCriteria(new RootCriteria()
                                    .leftJoin(Child.class, Root::getId, Child::getParentId).setOrderBy("id")
                                    .setPageNum(number).setPageSize(1L).setSearchCountFlag(true));
                            assertEquals(3L, page.getTotal());
                            assertEquals(number, page.getRecords().getFirst().id);
                            assertEquals(number == 1 ? 2 : number == 2 ? 1 : 0, page.getRecords().getFirst().children.size());
                        }
                        assertEquals(3, repository.countByCriteria(new RootCriteria()
                                .leftJoin(Child.class, Root::getId, Child::getParentId)));
                        assertEquals(2, repository.getByCriteria(new RootCriteria()
                                .leftJoin(Child.class, Root::getId, Child::getParentId).setOrderBy("id")).children.size());
                        var right = repository.findByCriteria(new RootCriteria()
                                .rightJoin(Child.class, Root::getId, Child::getParentId));
                        var unmatched = right.stream().filter(root -> root.id == null).toList();
                        assertEquals(3, unmatched.size());
                        assertNotSame(unmatched.get(0), unmatched.get(1));
                        assertNull(unmatched.getFirst().name);
                        assertEquals(1, unmatched.getFirst().children.size());
                        assertEquals(5, repository.countByCriteria(new RootCriteria()
                                .rightJoin(Child.class, Root::getId, Child::getParentId)));
                        var rightPage = repository.findPageByCriteria(new RootCriteria()
                                .rightJoin(Child.class, Root::getId, Child::getParentId)
                                .setPageNum(3L).setPageSize(2L).setSearchCountFlag(true));
                        assertEquals(5L, rightPage.getTotal());
                        assertEquals(1, rightPage.getRecords().size());
                        assertEquals(3, repository.findByCriteria(new RootCriteria().crossJoin(new JoinCondition()
                                .setTargetCriteria(new ChildCriteria().equals("id", 11L)).setResultProperty("children"))).size());
                        var singleAssociation = repository.findByCriteria(new RootCriteria()
                                .leftJoin(new JoinCondition().setTargetCriteria(EmptyCriteria.of(Child.class))
                                        .setSourceProperty("id").setCondition(Condition.Compare.EQUAL).setTargetProperty("parentId")
                                        .setResultProperty("child")));
                        assertTrue(Set.of(11L, 12L).contains(singleAssociation.getFirst().child.id));
                        Root clearName = new Root();
                        assertEquals(1, repository.updateCountByCriteria(clearName, new RootCriteria()
                                .equals("id", 3L).setNullValueMode(NullValueMode.WRITE_NULL)));
                        assertNull(repository.getEntityById(3L).getName());
                        assertEquals(Boolean.FALSE, repository.getEntityById(3L).getDeleted());
                    }
                });
    }

    @Getter
    @Setter
    @TableName("criteria_root")
    public static class Root implements IIdEntity<Long, Root> {
        @TableId
        private Long id;
        private String name;
        @TableLogic(value = "false", delval = "true")
        private Boolean deleted;
        @TableField(exist = false)
        private List<Child> children;
        @TableField(exist = false)
        private Child child;

        @Override
        public Root randomEntity() {
            return this;
        }

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("children", Child.class, "id", "parentId"),
                    EntityAssociation.one("child", Child.class, "name", "name"));
        }
    }

    @Getter
    @Setter
    @TableName("criteria_child")
    public static class Child implements IIdEntity<Long, Child> {
        @TableId
        private Long id;
        private Long parentId;
        private String name;
        @TableLogic(value = "false", delval = "true")
        private Boolean deleted;
        @TableField(exist = false)
        private Root root;

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.one("root", Root.class, "parentId", "id"));
        }

        @Override
        public Child randomEntity() {
            return this;
        }
    }

    public static class RootCriteria extends FullTypeCriteria<Root, RootCriteria> {
    }

    public static class ChildCriteria extends FullTypeCriteria<Child, ChildCriteria> {
    }

    @Getter
    @Setter
    @TableName("criteria_root")
    public static class SetRoot implements IIdEntity<Long, SetRoot> {
        @TableId
        private Long id;
        private String name;
        @TableLogic(value = "false", delval = "true")
        private Boolean deleted;
        @TableField(exist = false)
        private Set<Child> childSet;

        @Override
        public SetRoot randomEntity() {
            return this;
        }

        @Override
        public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("childSet", Child.class, "id", "parentId"));
        }
    }

    public static class SetRootCriteria extends FullTypeCriteria<SetRoot, SetRootCriteria> {
    }

    public interface SetRootMapper extends MPJBaseMapper<SetRoot> {
    }

    static class SetRootRepository extends MybatisPlusRepository<SetRoot, SetRootCriteria, SetRootMapper> {
        SetRootRepository(SetRootMapper mapper) {
            baseMapper = mapper;
        }
    }

    public interface RootMapper extends MPJBaseMapper<Root> {
    }

    public interface ChildMapper extends MPJBaseMapper<Child> {
    }

    static class RootRepository extends MybatisPlusRepository<Root, RootCriteria, RootMapper> {
        RootRepository(RootMapper mapper) {
            baseMapper = mapper;
        }
    }
}
