package vip.isass.framework.nocode;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.entity.IEntity;
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableMetaRegistrarTest {

    @Test
    void completesLogicDeleteFieldMetadataWithBooleanValues() throws Exception {
        Configuration configuration = new Configuration();
        TableInfo tableInfo = new TableInfo(configuration, LogicDeleteEntity.class);
        TableFieldInfo deleteFlag = new TableFieldInfo(
                new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()), tableInfo,
                LogicDeleteEntity.class.getDeclaredField("deleteFlag"),
                new Reflector(LogicDeleteEntity.class), false, false);
        setField(tableInfo, "fieldList", List.of(deleteFlag));

        tableMetas().put(LogicDeleteEntity.class,
                new TableMeta().tableName("logic_delete_entity").logicDeleteField("deleteFlag"));

        new TableMetaRegistrar().postTableInfo(tableInfo, configuration);

        assertTrue(tableInfo.isWithLogicDelete());
        assertTrue(deleteFlag.isLogicDelete());
        assertEquals("0", deleteFlag.getLogicNotDeleteValue());
        assertEquals("1", deleteFlag.getLogicDeleteValue());
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<?>, TableMeta> tableMetas() throws Exception {
        Field field = TableMetaRegistrar.class.getDeclaredField("metaMap");
        field.setAccessible(true);
        return (Map<Class<?>, TableMeta>) field.get(null);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    static class LogicDeleteEntity implements IEntity<LogicDeleteEntity>, ILogicDeleteEntity<LogicDeleteEntity> {

        private Boolean deleteFlag;

        @Override
        public Boolean getDeleteFlag() {
            return deleteFlag;
        }

        @Override
        public void setDeleteFlag(Boolean deleteFlag) {
            this.deleteFlag = deleteFlag;
        }

        @Override
        public LogicDeleteEntity randomEntity() {
            return this;
        }
    }
}
