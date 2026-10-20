// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import vip.isass.framework.common.criteria.ICriteria;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.entity.ILogicDeleteEntity;
import vip.isass.framework.common.entity.IParentIdEntity;
import vip.isass.framework.common.entity.ITenantEntity;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.common.support.JsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        tableInfo = new TableMetaRegistrar().postTableInfo(tableInfo, configuration);

        assertTrue(tableInfo.isWithLogicDelete());
        assertTrue(deleteFlag.isLogicDelete());
        assertEquals("0", deleteFlag.getLogicNotDeleteValue());
        assertEquals("1", deleteFlag.getLogicDeleteValue());
    }

    @Test
    void excludesResponseOnlyAssociationFieldsFromMybatisMetadata() throws Exception {
        Configuration configuration = new Configuration();
        TableInfo tableInfo = new TableInfo(configuration, ParentEntity.class);
        TableFieldInfo name = new TableFieldInfo(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()), tableInfo,
                ParentEntity.class.getDeclaredField("name"), new Reflector(ParentEntity.class), false, false);
        TableFieldInfo children = new TableFieldInfo(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()), tableInfo,
                ParentEntity.class.getDeclaredField("children"), new Reflector(ParentEntity.class), false, false);
        setField(tableInfo, "fieldList", new ArrayList<>(List.of(name, children)));
        tableMetas().put(ParentEntity.class, new TableMeta().tableName("parent_entity")
                .associationFields(Set.of("children")));

        tableInfo = new TableMetaRegistrar().postTableInfo(tableInfo, configuration);

        assertEquals(List.of("name"), tableInfo.getFieldList().stream().map(TableFieldInfo::getProperty).toList());
    }

    @Test
    void alwaysExcludesTreeProjectionsWhenEntityOverridesAssociations() throws Exception {
        Method registerEntity = TableMetaRegistrar.class.getDeclaredMethod("registerEntity", Class.class);
        registerEntity.setAccessible(true);
        registerEntity.invoke(null, TreeEntityWithCustomAssociation.class);

        assertEquals(Set.of("detail", "parent", "children"),
                tableMetas().get(TreeEntityWithCustomAssociation.class).associationFields());
    }

    @Test
    void marksTenantAndAppFieldsForInsertFill() throws Exception {
        Method registerEntity = TableMetaRegistrar.class.getDeclaredMethod("registerEntity", Class.class);
        registerEntity.setAccessible(true);
        registerEntity.invoke(null, TenantAppEntity.class);

        TableMeta meta = tableMetas().get(TenantAppEntity.class);
        assertEquals(com.baomidou.mybatisplus.annotation.FieldFill.INSERT, meta.fillFields().get("tenantId"));
        assertEquals(com.baomidou.mybatisplus.annotation.FieldFill.INSERT, meta.fillFields().get("appId"));
    }

    @Test
    void registersMetadataForRepositoryOnlyEntity() throws Exception {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registry.registerBeanDefinition("repositoryOnly", new RootBeanDefinition(RepositoryOnly.class));

        new TableMetaRegistrar().postProcessBeanDefinitionRegistry(registry);

        assertEquals("internal_repository_entity", tableMetas().get(RepositoryEntity.class).tableName());
    }

    @Test
    void ignoresServiceWithoutRepositoryBean() throws Exception {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        registry.registerBeanDefinition("serviceOnly", new RootBeanDefinition(ServiceOnly.class));

        new TableMetaRegistrar().postProcessBeanDefinitionRegistry(registry);

        assertNull(tableMetas().get(ServiceEntity.class));
    }

    @Test
    void directResultMapRestoresNestedJsonGenericsAndExcludesAssociations() throws Exception {
        Jackson3TypeHandler.setObjectMapper(JsonUtil.DEFAULT_INSTANCE);
        Configuration configuration = new Configuration();
        TableInfo tableInfo = jsonTable(configuration);
        new TableMetaRegistrar().postTableInfo(tableInfo, configuration);
        ResultMap resultMap = configuration.getResultMap(tableInfo.getResultMap());
        assertFalse(resultMap.getMappedProperties().contains("related"));
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString("selected_path")).thenReturn("""
                [{"id":"root","remark":"用户说明","children":[{"id":"leaf","remark":""}]}]
                """);
        ResultMapping selectedPath = resultMap.getResultMappings().stream()
                .filter(mapping -> mapping.getProperty().equals("selectedPath"))
                .findFirst()
                .orElseThrow();
        List<?> values = (List<?>) selectedPath.getTypeHandler().getResult(resultSet, "selected_path");
        JsonSnapshot root = assertInstanceOf(JsonSnapshot.class, values.getFirst());
        assertEquals("用户说明", root.remark);
        assertEquals("leaf", root.children.getFirst().id);
        assertEquals("", root.children.getFirst().remark);
        when(resultSet.getString("selected_path")).thenReturn("[]");
        assertEquals(List.of(), selectedPath.getTypeHandler().getResult(resultSet, "selected_path"));
        when(resultSet.getString("selected_path")).thenReturn(null);
        assertNull(selectedPath.getTypeHandler().getResult(resultSet, "selected_path"));

        when(resultSet.getString("named_paths")).thenReturn("{\"primary\":[{\"id\":\"leaf\",\"remark\":null}]}");
        ResultMapping namedPaths = resultMap.getResultMappings().stream()
                .filter(mapping -> mapping.getProperty().equals("namedPaths"))
                .findFirst()
                .orElseThrow();
        Map<?, ?> paths = (Map<?, ?>) namedPaths.getTypeHandler().getResult(resultSet, "named_paths");
        JsonSnapshot leaf = assertInstanceOf(JsonSnapshot.class, ((List<?>) paths.get("primary")).getFirst());
        assertEquals("leaf", leaf.id);
        assertNull(leaf.remark);
    }

    @Test
    void keepsAnExplicitResultMapInsteadOfReplacingIt() throws Exception {
        Configuration configuration = new Configuration();
        TableInfo tableInfo = jsonTable(configuration);
        String id = "explicit-json-map";
        configuration.addResultMap(new ResultMap.Builder(configuration, id, JsonEntity.class, List.of()).build());
        setField(tableInfo, "resultMap", id);
        new TableMetaRegistrar().postTableInfo(tableInfo, configuration);
        assertEquals(id, tableInfo.getResultMap());
    }

    private static TableInfo jsonTable(Configuration configuration) throws Exception {
        TableInfo tableInfo = new TableInfo(configuration, JsonEntity.class);
        tableMetas().put(JsonEntity.class, new TableMeta().tableName("json_entity")
                .associationFields(Set.of("related")));
        List<TableFieldInfo> fields = new ArrayList<>();
        var registrar = new TableMetaRegistrar();
        for (String name : List.of("selectedPath", "namedPaths", "related")) {
            TableFieldInfo field = new TableFieldInfo(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig()),
                    tableInfo, JsonEntity.class.getDeclaredField(name), new Reflector(JsonEntity.class), false, false);
            fields.add(registrar.postFieldInfo(field, configuration));
        }
        setField(tableInfo, "fieldList", fields);
        return tableInfo;
    }

    public static class JsonSnapshot {
        public String id;
        public String remark;
        public List<JsonSnapshot> children;
    }

    static class JsonEntity implements IEntity<JsonEntity> {
        private List<JsonSnapshot> selectedPath;
        private Map<String, List<JsonSnapshot>> namedPaths;
        private List<JsonSnapshot> related;

        @Override
        public JsonEntity randomEntity() {
            return this;
        }
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

    static class ParentEntity implements IIdEntity<Long, ParentEntity> {
        private Long id;
        private String name;
        private List<ChildEntity> children;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public ParentEntity randomEntity() { return this; }
        @Override public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.many("children", ChildEntity.class, "id", "parentId"));
        }
    }

    static class TenantAppEntity implements IIdEntity<Long, TenantAppEntity>, ITenantEntity<Long, TenantAppEntity> {
        private Long id;
        private Long tenantId;
        private Long appId;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public Long getTenantId() { return tenantId; }
        @Override public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
        @Override public TenantAppEntity randomEntity() { return this; }
    }

    static class ChildEntity implements IIdEntity<Long, ChildEntity> {
        private Long id;
        private Long parentId;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public ChildEntity randomEntity() { return this; }
    }

    static class TreeEntityWithCustomAssociation
            implements IIdEntity<Long, TreeEntityWithCustomAssociation>,
            IParentIdEntity<Long, TreeEntityWithCustomAssociation> {
        private Long id;
        private Long parentId;
        private TreeEntityWithCustomAssociation parent;
        private List<TreeEntityWithCustomAssociation> children;
        private ChildEntity detail;

        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public Long getParentId() { return parentId; }
        @Override public void setParentId(Long parentId) { this.parentId = parentId; }
        @Override public TreeEntityWithCustomAssociation getParent() { return parent; }
        @Override public void setParent(TreeEntityWithCustomAssociation parent) { this.parent = parent; }
        @Override public List<TreeEntityWithCustomAssociation> getChildren() { return children; }
        @Override public void setChildren(List<TreeEntityWithCustomAssociation> children) { this.children = children; }
        @Override public TreeEntityWithCustomAssociation randomEntity() { return this; }
        @Override public List<EntityAssociation> associations() {
            return List.of(EntityAssociation.one("detail", ChildEntity.class, "id", "parentId"));
        }
    }

    static class RepositoryEntity implements IEntity<RepositoryEntity> {
        @Override public RepositoryEntity randomEntity() { return this; }
        @Override public String tableName() { return "internal_repository_entity"; }
    }

    static class RepositoryCriteria implements ICriteria<RepositoryEntity, RepositoryCriteria> {
    }

    static class RepositoryOnly implements IRepository<RepositoryEntity, RepositoryCriteria> {
    }

    static class ServiceEntity implements IEntity<ServiceEntity> {
        @Override public ServiceEntity randomEntity() { return this; }
        @Override public String tableName() { return "internal_service_entity"; }
    }

    static class ServiceCriteria implements ICriteria<ServiceEntity, ServiceCriteria> {
    }

    static class ServiceOnly {
        public IRepository<ServiceEntity, ServiceCriteria> getRepository() {
            return null;
        }
    }
}
