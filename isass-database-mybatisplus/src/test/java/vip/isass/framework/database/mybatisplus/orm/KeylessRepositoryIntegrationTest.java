// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.orm;

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
import vip.isass.framework.common.criteria.WhereCondition;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.database.mybatisplus.DatabaseMybatisPlusAutoConfiguration;
import vip.isass.framework.security.data.DataReadContext;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real SQL against a temporary keyless H2 table, never the business database. */
class KeylessRepositoryIntegrationTest {

    @Test
    void executesKeylessCrudAndPreservesReadPoliciesAndLogicalDelete() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:keyless_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class,
                        MybatisPlusJoinAutoConfiguration.class))
                .withUserConfiguration(DatabaseMybatisPlusAutoConfiguration.class)
                .withBean(DataSource.class, () -> dataSource)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    try (var connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                        statement.execute("CREATE TABLE keyless_setting (code VARCHAR(64), content VARCHAR(255), deleted BOOLEAN)");
                        statement.execute("INSERT INTO keyless_setting VALUES ('visible','before',false),('hidden','secret',false),('removed','old',true)");
                    }
                    SqlSessionFactory factory = context.getBean(SqlSessionFactory.class);
                    factory.getConfiguration().addMapper(SettingMapper.class);
                    try (var session = factory.openSession(true)) {
                        SettingRepository repository = new SettingRepository(session.getMapper(SettingMapper.class));
                        Setting added = new Setting();
                        added.setCode("new");
                        added.setContent("new content");
                        added.setDeleted(false);
                        assertTrue(repository.add(added));
                        assertEquals(3, repository.countByCriteria(new SettingCriteria()));
                        assertEquals(3L, repository.findPageByCriteria(new SettingCriteria()).getTotal());
                        assertTrue(repository.isPresentByCriteria(new SettingCriteria().equals("code", "visible")));
                        assertFalse(repository.isPresentByCriteria(new SettingCriteria().equals("code", "removed")));
                        try (var ignored = DataReadContext.open(type -> List.of(WhereCondition.eq("code", "visible")))) {
                            assertEquals(1, repository.countByCriteria(new SettingCriteria()));
                            assertEquals(1L, repository.findPageByCriteria(new SettingCriteria().setReturnField("content")).getTotal());
                            assertFalse(repository.isPresentByCriteria(new SettingCriteria().equals("code", "hidden")));
                        }
                        Setting update = new Setting();
                        update.setContent("after");
                        assertEquals(1, repository.updateCountByCriteria(update, new SettingCriteria().equals("code", "visible")));
                        assertEquals("after", repository.getByCriteria(new SettingCriteria().equals("code", "visible")).getContent());
                        assertEquals(1, repository.deleteCountByCriteria(new SettingCriteria().equals("code", "new")));
                        assertEquals(2, repository.countByCriteria(new SettingCriteria()));

                        assertThrows(UnsupportedOperationException.class, () -> repository.getEntityById(1L));
                        assertThrows(UnsupportedOperationException.class, () -> repository.isPresentById(1L));
                        assertThrows(UnsupportedOperationException.class, () -> repository.updateById(update));
                        assertThrows(UnsupportedOperationException.class, () -> repository.updateAllColumnsById(update));
                        assertThrows(UnsupportedOperationException.class, () -> repository.deleteById(1L));
                        assertThrows(UnsupportedOperationException.class, () -> repository.deleteCountByIds(List.of(1L)));
                        assertThrows(UnsupportedOperationException.class, () -> repository.getEntityByIdForUpdate(1L));
                    }
                });
    }

    @Getter
    @Setter
    @TableName("keyless_setting")
    public static class Setting implements IEntity<Setting> {
        private String code;
        private String content;
        @TableLogic(value = "false", delval = "true")
        private Boolean deleted;

        @Override
        public Setting randomEntity() {
            return this;
        }
    }

    public static class SettingCriteria extends FullTypeCriteria<Setting, SettingCriteria> {
    }

    public interface SettingMapper extends MPJBaseMapper<Setting> {
    }

    static class SettingRepository extends MybatisPlusRepository<Setting, SettingCriteria, SettingMapper> {
        SettingRepository(SettingMapper mapper) {
            baseMapper = mapper;
        }
    }
}
