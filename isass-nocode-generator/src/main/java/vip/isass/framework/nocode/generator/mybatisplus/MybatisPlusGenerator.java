// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.INameConvert;
import com.baomidou.mybatisplus.generator.config.builder.CustomFile;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.TemplateHashModel;
import freemarker.template.Version;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import vip.isass.framework.nocode.generator.association.GeneratorAssociation;
import vip.isass.framework.nocode.generator.association.EntityRelationDefinition;
import vip.isass.framework.nocode.generator.model.EntityModelDefinition;
import vip.isass.framework.nocode.generator.model.EntityFieldDefinition;
import vip.isass.framework.nocode.generator.association.EntityRelationValidator;
import vip.isass.framework.nocode.generator.association.TableAssociationParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.TreeMap;
import java.util.regex.Pattern;

@Slf4j
public class MybatisPlusGenerator {

    private static final Pattern CONTEXT_NAME = Pattern.compile("[a-z][a-z0-9]*");
    private static final Pattern ENTITY_NAME = Pattern.compile("[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

    record GenerationScope(String context, String domain, String subdomain) {
    }

    private static final INameConvert CONTEXT_FREE_ENTITY_NAME_CONVERT = new INameConvert() {
        @Override
        public String entityNameConvert(TableInfo tableInfo) {
            String tableName = NamingStrategy.removePrefix(
                    tableInfo.getName(), tableInfo.getStrategyConfig().getTablePrefix());
            int contextSeparator = tableName.indexOf('_');
            String entityPart = contextSeparator < 0 ? tableName : tableName.substring(contextSeparator + 1);
            return NamingStrategy.capitalFirst(NamingStrategy.underlineToCamel(entityPart));
        }

        @Override
        public String propertyNameConvert(TableField field) {
            return NamingStrategy.underlineToCamel(field.getName());
        }
    };

    @SneakyThrows
    public static void generate(MybatisPlusGeneratorMeta meta) {
        List<MybatisPlusGeneratorMeta> contextMetas = generationMetas(meta);
        List<MybatisPlusGeneratorMeta> catalogMetas = generationMetas(meta, false);
        Map<String, String> entityPackages = entityPackages(catalogMetas);
        validateRelations(meta, catalogMetas);
        contextMetas.forEach(contextMeta -> {
            generateApiFiles(contextMeta, entityPackages);
            generateServiceFiles(contextMeta);
        });
    }

    /**
     * 按服务表名的第二段和表注释领域标记划分限界上下文、领域与可选子域，例如
     * {@code bsp_auth_role} + {@code [--domain:authorization;--subdomain:role]} 对应
     * {@code bsp.auth.domain.authorization.role}。
     */
    private static List<MybatisPlusGeneratorMeta> generationMetas(MybatisPlusGeneratorMeta meta) throws SQLException {
        return generationMetas(meta, true);
    }

    private static List<MybatisPlusGeneratorMeta> generationMetas(
            MybatisPlusGeneratorMeta meta, boolean selectedOnly) throws SQLException {
        Map<GenerationScope, List<String>> tablesByScope = new TreeMap<>(Comparator
                .comparing(GenerationScope::context)
                .thenComparing(GenerationScope::domain)
                .thenComparing(GenerationScope::subdomain, Comparator.nullsFirst(String::compareTo)));
        try (Connection connection = DriverManager.getConnection(
                meta.getDataSourceUrl(), meta.getDataSourceUserName(), meta.getDataSourcePassword());
             ResultSet tables = connection.getMetaData().getTables(
                     connection.getCatalog(), meta.getSchemaName(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (!isTableSelected(tableName,
                        selectedOnly ? meta.getIncludeTables() : null, meta.getExcludeTables())) {
                    continue;
                }
                GenerationScope scope = modelScopeOf(tableName, meta);
                if (scope == null) continue;
                tablesByScope.computeIfAbsent(scope, ignored -> new ArrayList<>()).add(tableName);
            }
        }
        if (tablesByScope.isEmpty()) {
            throw new IllegalStateException(
                    "未发现符合 service_context_entity 命名规则且有模型声明的数据库表");
        }
        return tablesByScope.entrySet().stream()
                .map(entry -> metaForGenerationScope(meta, entry.getKey(), entry.getValue()))
                .toList();
    }

    static boolean isTableSelected(String tableName, String[] includeTables, String[] excludeTables) {
        if (includeTables != null && includeTables.length > 0) {
            return matchesAny(tableName, includeTables);
        }
        return excludeTables == null || excludeTables.length == 0 || !matchesAny(tableName, excludeTables);
    }

    static GenerationScope modelScopeOf(String tableName, MybatisPlusGeneratorMeta meta) {
        if (contextOf(tableName, meta.getTablePrefix()) == null) {
            return null;
        }
        EntityModelDefinition model = modelOf(entityNameOf(tableName, meta.getTablePrefix()), meta);
        return new GenerationScope(contextOf(tableName, meta.getTablePrefix()), model.domain(), model.subdomain());
    }

    private static EntityModelDefinition modelOf(String entity, MybatisPlusGeneratorMeta meta) {
        List<EntityModelDefinition> definitions = meta.getModels().stream()
                .filter(model -> model.entityName().equals(entity)).toList();
        if (definitions.size() != 1) {
            throw new IllegalStateException("实体必须有且只有一个 EntityModelDefinition: " + entity);
        }
        return definitions.getFirst();
    }

    private static boolean matchesAny(String tableName, String[] patterns) {
        for (String pattern : patterns) {
            if (tableName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    static String contextOf(String tableName, String[] tablePrefixes) {
        for (String tablePrefix : tablePrefixes) {
            if (!tableName.startsWith(tablePrefix)) {
                continue;
            }
            String suffix = tableName.substring(tablePrefix.length());
            int contextSeparator = suffix.indexOf('_');
            if (contextSeparator > 0 && contextSeparator < suffix.length() - 1) {
                String context = suffix.substring(0, contextSeparator);
                String entity = suffix.substring(contextSeparator + 1);
                if (CONTEXT_NAME.matcher(context).matches() && ENTITY_NAME.matcher(entity).matches()) {
                    return context;
                }
            }
        }
        return null;
    }


    static String entityNameOf(String tableName, String[] tablePrefixes) {
        String suffix = tableName;
        for (String tablePrefix : tablePrefixes) {
            if (suffix.startsWith(tablePrefix)) {
                suffix = suffix.substring(tablePrefix.length());
                break;
            }
        }
        int contextSeparator = suffix.indexOf('_');
        String entityPart = contextSeparator < 0 ? suffix : suffix.substring(contextSeparator + 1);
        return NamingStrategy.capitalFirst(NamingStrategy.underlineToCamel(entityPart));
    }

    private static Map<String, String> entityPackages(List<MybatisPlusGeneratorMeta> metas) {
        Map<String, String> result = new TreeMap<>();
        for (MybatisPlusGeneratorMeta meta : metas) {
            String entityPackage = meta.getPackageName() + "." + meta.getContext() + ".domain.model.entity";
            for (String table : meta.getIncludeTables()) {
                String entityName = entityNameOf(table, meta.getTablePrefix());
                String existingPackage = result.putIfAbsent(entityName, entityPackage);
                if (existingPackage != null && !existingPackage.equals(entityPackage)) {
                    throw new IllegalStateException("实体简单类名 " + entityName + " 同时属于 "
                            + existingPackage + " 和 " + entityPackage
                            + "，关联目标无法唯一解析，请调整实体命名");
                }
            }
        }
        return result;
    }

    private static void validateRelations(
            MybatisPlusGeneratorMeta meta, List<MybatisPlusGeneratorMeta> catalogMetas) throws SQLException {
        Map<String, Map<String, Integer>> entityColumns = new TreeMap<>();
        try (Connection connection = DriverManager.getConnection(
                meta.getDataSourceUrl(), meta.getDataSourceUserName(), meta.getDataSourcePassword())) {
            for (MybatisPlusGeneratorMeta catalogMeta : catalogMetas) {
                for (String table : catalogMeta.getIncludeTables()) {
                    Map<String, Integer> columns = new TreeMap<>();
                    try (ResultSet fields = connection.getMetaData().getColumns(
                            connection.getCatalog(), meta.getSchemaName(), table, "%")) {
                        while (fields.next()) {
                            columns.put(NamingStrategy.underlineToCamel(fields.getString("COLUMN_NAME")),
                                    fields.getInt("DATA_TYPE"));
                        }
                    }
                    entityColumns.put(entityNameOf(table, meta.getTablePrefix()), columns);
                }
            }
        }
        EntityRelationValidator.validate(
                meta.getRelations(), meta.getTreeCascadeDeleteEntities(), entityColumns);
        for (EntityModelDefinition model : meta.getModels()) {
            Map<String, Integer> columns = entityColumns.get(model.entityName());
            if (columns == null) {
                throw new IllegalArgumentException("模型声明不存在于数据库目录: " + model.entityName());
            }
            for (EntityFieldDefinition field : model.fields()) {
                if (!columns.containsKey(field.propertyName())) {
                    throw new IllegalArgumentException("字段声明不存在: " + model.entityName() + "." + field.propertyName());
                }
            }
        }
    }

    private static MybatisPlusGeneratorMeta metaForGenerationScope(
            MybatisPlusGeneratorMeta source, GenerationScope scope, List<String> includeTables) {
        return new MybatisPlusGeneratorMeta()
                .setDbType(source.getDbType())
                .setDataSourceUserName(source.getDataSourceUserName())
                .setDataSourcePassword(source.getDataSourcePassword())
                .setDataSourceUrl(source.getDataSourceUrl())
                .setSchemaName(source.getSchemaName())
                .setOutputDir(source.getOutputDir())
                .setContext(generationContext(
                        source.getContext(), scope.context(), scope.domain(), scope.subdomain()))
                .setPackageName(source.getPackageName())
                .setTablePrefix(source.getTablePrefix())
                .setIncludeTables(includeTables.toArray(String[]::new))
                .setApiOutputDir(source.getApiOutputDir())
                .setServiceOutputDir(source.getServiceOutputDir())
                .setRelations(source.getRelations())
                .setModels(source.getModels())
                .setTreeCascadeDeleteEntities(source.getTreeCascadeDeleteEntities())
                .setEntityFileOverride(source.isEntityFileOverride())
                .setCriteriaFileOverride(source.isCriteriaFileOverride())
                .setMapperFileOverride(source.isMapperFileOverride())
                .setMapperXmlFileOverride(source.isMapperXmlFileOverride())
                .setRepositoryFileOverride(source.isRepositoryFileOverride())
                .setServiceInterfaceFileOverride(source.isServiceInterfaceFileOverride())
                .setLocalServiceFileOverride(source.isLocalServiceFileOverride());
    }

    static String generationContext(String serviceContext, String boundedContext, String domain) {
        return generationContext(serviceContext, boundedContext, domain, null);
    }

    static String generationContext(
            String serviceContext, String boundedContext, String domain, String subdomain) {
        String domainContext = serviceContext + "." + boundedContext + ".domain." + domain;
        return subdomain == null ? domainContext : domainContext + "." + subdomain;
    }

    @SneakyThrows
    private static void generateApiFiles(MybatisPlusGeneratorMeta meta, Map<String, String> entityPackages) {
        String outputDir = meta.getApiOutputDir() != null
                ? meta.getApiOutputDir() + "/src/main/java"
                : meta.getOutputDir() + "/src/main/java";
        String basePackage = meta.getPackageName() + "." + meta.getContext();
        String servicePackageName = applicationServicePackageName(meta);
        String boundedContextName = boundedContextName(meta);
        BeansWrapper wrapper = new BeansWrapperBuilder(new Version("2.3.28")).build();
        TemplateHashModel staticModels = wrapper.getStaticModels();

        FastAutoGenerator.create(meta.getDataSourceUrl(), meta.getDataSourceUserName(), meta.getDataSourcePassword())
                .globalConfig(builder -> builder
                        .author("isass")
                        .outputDir(outputDir)
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                        .dateType(DateType.TIME_PACK))
                .dataSourceConfig(builder -> builder
                        .schema(meta.getSchemaName())
                        .typeConvertHandler(new TypeConvertHandler()))
                .strategyConfig(builder -> {
                    builder
                            // 是否跳过视图
                            .enableSkipView()
                            // 是否大写命名
                            .enableCapitalMode()
                            // 表前缀
                            .addTablePrefix(meta.getTablePrefix());
                    if (meta.getIncludeTables() != null && meta.getIncludeTables().length > 0) {
                        builder.addInclude(meta.getIncludeTables());
                    } else if (meta.getExcludeTables() != null && meta.getExcludeTables().length > 0) {
                        builder.addExclude(meta.getExcludeTables());
                    }
                    builder
                            // 取消内置的 controller 模板
                            .controllerBuilder().disable()
                            // 取消内置的 service 模板
                            .serviceBuilder().disable()
                            // 取消内置的 mapper 模板
                            .mapperBuilder().disable()
                            // 取消内置的 entity 模板
                            .entityBuilder()
                            .nameConvert(CONTEXT_FREE_ENTITY_NAME_CONVERT).disable()
                            // 乐观锁名称
                            .versionPropertyName("version")
                            .versionColumnName("version")
                            // 逻辑删除名称
                            .logicDeletePropertyName("deleteFlag")
                            .logicDeleteColumnName("delete_flag");
                })
                .packageConfig(builder -> builder
                        .parent(meta.getPackageName())
                        .moduleName(meta.getContext()))
                .injectionConfig(builder -> {
                    try {
                        builder
                                .beforeOutputFile((table, objectMap) -> {
                                    EntityModelDefinition model = modelOf(table.getEntityName(), meta);
                                    Map<String, EntityFieldDefinition> fields = model.fields().stream()
                                            .collect(Collectors.toMap(EntityFieldDefinition::propertyName, field -> field));
                                    Set<String> properties = table.getFields().stream()
                                            .map(field -> field.getPropertyName()).collect(Collectors.toSet());
                                    if (!properties.containsAll(fields.keySet())) {
                                        throw new IllegalArgumentException("字段声明不存在: " + table.getEntityName());
                                    }
                                    objectMap.put("modelFields", fields);
                                    objectMap.put("tenantIsolation", model.tenantIsolation());
                                    List<GeneratorAssociation> associations =
                                            meta.getRelations().stream()
                                                    .filter(relation -> relation.sourceEntity().equals(table.getEntityName()))
                                                    .map(EntityRelationDefinition::association)
                                                    .toList();
                                    objectMap.put("associations", associations);
                                    String entityPackage = meta.getPackageName() + "." + meta.getContext()
                                            + ".domain.model.entity";
                                    objectMap.put("associationImports", associations.stream()
                                            .map(association -> {
                                                String targetPackage = entityPackages.get(association.targetEntity());
                                                return targetPackage == null || targetPackage.equals(entityPackage)
                                                        ? null
                                                        : targetPackage + "." + association.targetEntity();
                                            })
                                            .filter(importName -> importName != null)
                                            .distinct()
                                            .toList());
                                    objectMap.put("treeCascadeDelete",
                                            meta.getTreeCascadeDeleteEntities().contains(table.getEntityName()));
                                    objectMap.put("tableDescription",
                                            TableAssociationParser.description(table.getComment()));
                                })
                                .customMap(MapUtil.<String, Object>builder()
                                        .put("context", meta.getContext())
                                        .put("boundedContextName", boundedContextName)
                                        .put("package", meta.getPackageName())
                                        .put("serviceRootPackageName", serviceRootPackageName(meta))
                                        .put("entityPackageName", basePackage + ".domain.model.entity")
                                        .put("criteriaPackageName", basePackage + ".domain.model.criteria")
                                        .put("mapperPackageName", basePackage + ".infrastructure.persistence.mybatisplus")
                                        .put("servicePackageName", servicePackageName)
                                        .put("tablePrefix", meta.getTablePrefix())

                                        .put("idEntity", staticModels.get("vip.isass.framework.common.entity.IIdEntity"))
                                        .put("parentIdEntity", staticModels.get("vip.isass.framework.common.entity.IParentIdEntity"))
                                        .put("logicDeleteEntity", staticModels.get("vip.isass.framework.common.entity.ILogicDeleteEntity"))
                                        .put("tenantEntity", staticModels.get("vip.isass.framework.common.entity.ITenantEntity"))
                                        .put("traceEntity", staticModels.get("vip.isass.framework.common.entity.ITraceEntity"))
                                        .put("versionEntity", staticModels.get("vip.isass.framework.common.entity.IVersionEntity"))
                                        .build()
                                )
                                .customFile(CollUtil.newArrayList(
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/entity.java.ftl")
                                                        .packageName("domain.model.entity")
                                                        .fileName(".java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isEntityFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/criteria.java.ftl")
                                                        .packageName("domain.model.criteria")
                                                        .fileName("Criteria.java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isCriteriaFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/IService.java.ftl")
                                                        .filePath(outputDir)
                                                        .packageName(servicePackageName)
                                                        .fileName("Service.java")
                                                        .formatNameFunction(tableInfo -> "I" + tableInfo.getEntityName()),
                                                meta.isServiceInterfaceFileOverride())
                                ));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    @SneakyThrows
    private static void generateServiceFiles(MybatisPlusGeneratorMeta meta) {
        String outputDir = meta.getServiceOutputDir() != null
                ? meta.getServiceOutputDir() + "/src/main/java"
                : meta.getOutputDir() + "/src/main/java";
        String basePackage = meta.getPackageName() + "." + meta.getContext();
        String servicePackageName = applicationServicePackageName(meta);
        String boundedContextName = boundedContextName(meta);
        BeansWrapper wrapper = new BeansWrapperBuilder(new Version("2.3.28")).build();
        TemplateHashModel staticModels = wrapper.getStaticModels();

        FastAutoGenerator.create(meta.getDataSourceUrl(), meta.getDataSourceUserName(), meta.getDataSourcePassword())
                .globalConfig(builder -> builder
                        .author("isass")
                        .outputDir(outputDir)
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                        .dateType(DateType.TIME_PACK))
                .dataSourceConfig(builder -> builder
                        .schema(meta.getSchemaName())
                        .typeConvertHandler(new TypeConvertHandler()))
                .strategyConfig(builder -> {
                    builder
                            // 是否跳过视图
                            .enableSkipView()
                            // 是否大写命名
                            .enableCapitalMode()
                            // 表前缀
                            .addTablePrefix(meta.getTablePrefix());
                    if (meta.getIncludeTables() != null && meta.getIncludeTables().length > 0) {
                        builder.addInclude(meta.getIncludeTables());
                    } else if (meta.getExcludeTables() != null && meta.getExcludeTables().length > 0) {
                        builder.addExclude(meta.getExcludeTables());
                    }
                    builder
                            // 取消内置的 controller 模板
                            .controllerBuilder().disable()
                            // 取消内置的 service 模板
                            .serviceBuilder().disable()
                            // 取消内置的 mapper 模板
                            .mapperBuilder().disable()
                            // 取消内置的 entity 模板
                            .entityBuilder().nameConvert(CONTEXT_FREE_ENTITY_NAME_CONVERT).disable()
                            // 乐观锁名称
                            .versionPropertyName("version")
                            .versionColumnName("version")
                            // 逻辑删除名称
                            .logicDeletePropertyName("deleteFlag")
                            .logicDeleteColumnName("delete_flag");
                })
                .packageConfig(builder -> builder
                        .parent(meta.getPackageName())
                        .moduleName(meta.getContext()))
                .injectionConfig(builder -> {
                    try {
                        builder
                                .beforeOutputFile((table, objectMap) -> {
                                    objectMap.put("tableDescription", TableAssociationParser.description(table.getComment()));
                                    objectMap.put("tenantIsolation", modelOf(table.getEntityName(), meta).tenantIsolation());
                                })
                                .customMap(MapUtil.<String, Object>builder()
                                        .put("context", meta.getContext())
                                        .put("boundedContextName", boundedContextName)
                                        .put("package", meta.getPackageName())
                                        .put("serviceRootPackageName", serviceRootPackageName(meta))
                                        .put("entityPackageName", basePackage + ".domain.model.entity")
                                        .put("criteriaPackageName", basePackage + ".domain.model.criteria")
                                        .put("repositoryPackageName", basePackage + ".domain.repository")
                                        .put("mapperPackageName", basePackage + ".infrastructure.persistence.mybatisplus")
                                        .put("servicePackageName", servicePackageName)
                                        .put("tablePrefix", meta.getTablePrefix())

                                        .put("idEntity", staticModels.get("vip.isass.framework.common.entity.IIdEntity"))
                                        .put("parentIdEntity", staticModels.get("vip.isass.framework.common.entity.IParentIdEntity"))
                                        .put("logicDeleteEntity", staticModels.get("vip.isass.framework.common.entity.ILogicDeleteEntity"))
                                        .put("tenantEntity", staticModels.get("vip.isass.framework.common.entity.ITenantEntity"))
                                        .put("traceEntity", staticModels.get("vip.isass.framework.common.entity.ITraceEntity"))
                                        .put("versionEntity", staticModels.get("vip.isass.framework.common.entity.IVersionEntity"))
                                        .build()
                                )
                                .customFile(CollUtil.newArrayList(
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/mapper.java.ftl")
                                                        .packageName("infrastructure.persistence.mybatisplus")
                                                        .fileName("Mapper.java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isMapperFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/mapper.xml.ftl")
                                                        .packageName("infrastructure.persistence.mybatisplus.xml")
                                                        .fileName("Mapper.xml")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isMapperXmlFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/repository.java.ftl")
                                                        .packageName("infrastructure.persistence.mybatisplus")
                                                        .fileName("Repository.java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isRepositoryFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/repositoryInterface.java.ftl")
                                                        .packageName("domain.repository")
                                                        .fileName(".java")
                                                        .formatNameFunction(tableInfo -> "I" + tableInfo.getEntityName() + "Repository"),
                                                false),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/localService.java.ftl")
                                                        .filePath(outputDir)
                                                        .packageName(servicePackageName)
                                                        .fileName("Service.java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isLocalServiceFileOverride())
                                ));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }

    private static CustomFile customFile(CustomFile.Builder builder, boolean fileOverride) {
        if (fileOverride) {
            builder.enableFileOverride();
        }
        return builder.build();
    }

    static String serviceRootPackageName(MybatisPlusGeneratorMeta meta) {
        String context = meta.getContext();
        int separator = context.indexOf('.');
        String serviceContext = separator < 0 ? context : context.substring(0, separator);
        return meta.getPackageName() + "." + serviceContext;
    }

    static String applicationServicePackageName(MybatisPlusGeneratorMeta meta) {
        String context = meta.getContext();
        String domainMarker = ".domain.";
        int domainMarkerIndex = context.indexOf(domainMarker);
        if (domainMarkerIndex < 0) {
            throw new IllegalStateException("生成上下文缺少 domain 层级: " + context);
        }
        String applicationContext = context.substring(0, domainMarkerIndex)
                + ".application."
                + context.substring(domainMarkerIndex + domainMarker.length());
        return meta.getPackageName() + "." + applicationContext + ".service";
    }

    private static String boundedContextName(MybatisPlusGeneratorMeta meta) {
        String context = meta.getContext();
        int serviceSeparator = context.indexOf('.');
        if (serviceSeparator < 0) {
            return context;
        }
        int domainSeparator = context.indexOf('.', serviceSeparator + 1);
        return domainSeparator < 0
                ? context.substring(serviceSeparator + 1)
                : context.substring(serviceSeparator + 1, domainSeparator);
    }



}
