// SPDX-License-Identifier: LGPL-3.0-only
package vip.isass.framework.database.mybatisplus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisParameterHandler;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.junit.jupiter.api.Test;
import vip.isass.framework.common.entity.IIdEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssignedBusinessIdTest {
    @Test
    void realMybatisInsertHandlerKeepsExplicitStringIdsForSingleAndBatchInserts() throws Exception {
        Method register = TableMetaRegistrar.class.getDeclaredMethod("registerEntity", Class.class);
        register.setAccessible(true);
        register.invoke(null, BusinessEntity.class);
        MybatisConfiguration configuration = new MybatisConfiguration();
        AtomicInteger generated = new AtomicInteger();
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig()
                .setDbConfig(new GlobalConfig.DbConfig())
                .setIdentifierGenerator(new IdentifierGenerator() {
                    @Override
                    public Number nextId(Object entity) {
                        generated.incrementAndGet();
                        return 42L;
                    }
                }));
        TableInfo table = TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, "test"),
                BusinessEntity.class);
        new TableMetaRegistrar().postTableInfo(table, configuration);
        assertEquals(IdType.ASSIGN_ID, table.getIdType());
        MappedStatement insert = new MappedStatement.Builder(configuration, "business.insert",
                new StaticSqlSource(configuration, "INSERT"), SqlCommandType.INSERT).build();
        BusinessEntity first = new BusinessEntity("content_creation");
        BusinessEntity second = new BusinessEntity("content_creation_tone");
        new MybatisParameterHandler(insert, first, insert.getBoundSql(first));
        new MybatisParameterHandler(insert, List.of(first, second), insert.getBoundSql(List.of(first, second)));
        assertEquals("content_creation", first.getId());
        assertEquals("content_creation_tone", second.getId());
        assertEquals(0, generated.get());
        BusinessEntity absent = new BusinessEntity(null);
        new MybatisParameterHandler(insert, absent, insert.getBoundSql(absent));
        assertEquals("42", absent.getId());
        assertEquals(1, generated.get());
    }

    static final class BusinessEntity implements IIdEntity<String, BusinessEntity> {
        private String id;
        BusinessEntity(String id) { this.id = id; }
        @Override public String getId() { return id; }
        @Override public void setId(String id) { this.id = id; }
        @Override public String tableName() { return "test_business"; }
    }
}
