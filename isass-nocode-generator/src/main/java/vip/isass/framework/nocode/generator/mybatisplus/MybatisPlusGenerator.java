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
import vip.isass.framework.nocode.generator.association.TableAssociationParser;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class MybatisPlusGenerator {

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
        generationMetas(meta).forEach(contextMeta -> {
            generateApiFiles(contextMeta);
            generateServiceFiles(contextMeta);
        });
    }

    /**
     * 按服务表名的第二段划分限界上下文，例如 {@code bsp_auth_access_key} 对应 {@code auth}。
     */
    private static List<MybatisPlusGeneratorMeta> generationMetas(MybatisPlusGeneratorMeta meta) throws SQLException {
        Map<String, List<String>> tablesByContext = new TreeMap<>();
        try (Connection connection = DriverManager.getConnection(
                meta.getDataSourceUrl(), meta.getDataSourceUserName(), meta.getDataSourcePassword());
             ResultSet tables = connection.getMetaData().getTables(
                     connection.getCatalog(), meta.getSchemaName(), "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (!isTableSelected(tableName, meta.getIncludeTables(), meta.getExcludeTables())) {
                    continue;
                }
                String context = contextOf(tableName, meta.getTablePrefix());
                if (context != null) {
                    tablesByContext.computeIfAbsent(context, ignored -> new ArrayList<>()).add(tableName);
                }
            }
        }
        if (tablesByContext.isEmpty()) {
            throw new IllegalStateException("未发现符合表前缀及限界上下文命名规则的数据库表");
        }
        return tablesByContext.entrySet().stream()
                .map(entry -> metaForContext(meta, entry.getKey(), entry.getValue()))
                .toList();
    }

    static boolean isTableSelected(String tableName, String[] includeTables, String[] excludeTables) {
        if (includeTables != null && includeTables.length > 0) {
            return matchesAny(tableName, includeTables);
        }
        return excludeTables == null || excludeTables.length == 0 || !matchesAny(tableName, excludeTables);
    }

    private static boolean matchesAny(String tableName, String[] patterns) {
        for (String pattern : patterns) {
            if (tableName.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String contextOf(String tableName, String[] tablePrefixes) {
        for (String tablePrefix : tablePrefixes) {
            if (!tableName.startsWith(tablePrefix)) {
                continue;
            }
            String suffix = tableName.substring(tablePrefix.length());
            int separator = suffix.indexOf('_');
            if (separator > 0) {
                return suffix.substring(0, separator);
            }
        }
        return null;
    }

    private static MybatisPlusGeneratorMeta metaForContext(
            MybatisPlusGeneratorMeta source, String context, List<String> includeTables) {
        return new MybatisPlusGeneratorMeta()
                .setDbType(source.getDbType())
                .setDataSourceUserName(source.getDataSourceUserName())
                .setDataSourcePassword(source.getDataSourcePassword())
                .setDataSourceUrl(source.getDataSourceUrl())
                .setSchemaName(source.getSchemaName())
                .setOutputDir(source.getOutputDir())
                .setContext(source.getContext() + "." + context)
                .setPackageName(source.getPackageName())
                .setTablePrefix(source.getTablePrefix())
                .setIncludeTables(includeTables.toArray(String[]::new))
                .setApiOutputDir(source.getApiOutputDir())
                .setServiceOutputDir(source.getServiceOutputDir())
                .setControllerPrefix(source.getControllerPrefix() + "/" + context)
                .setEntityFileOverride(source.isEntityFileOverride())
                .setCriteriaFileOverride(source.isCriteriaFileOverride())
                .setMapperFileOverride(source.isMapperFileOverride())
                .setMapperXmlFileOverride(source.isMapperXmlFileOverride())
                .setRepositoryFileOverride(source.isRepositoryFileOverride())
                .setServiceInterfaceFileOverride(source.isServiceInterfaceFileOverride())
                .setLocalServiceFileOverride(source.isLocalServiceFileOverride())
                .setControllerFileOverride(source.isControllerFileOverride());
    }

    @SneakyThrows
    private static void generateApiFiles(MybatisPlusGeneratorMeta meta) {
        String outputDir = meta.getApiOutputDir() != null
                ? meta.getApiOutputDir() + "/src/main/java"
                : meta.getOutputDir() + "/src/main/java";
        String basePackage = meta.getPackageName() + "." + meta.getContext();
        String boundedContextName = meta.getContext().substring(meta.getContext().lastIndexOf('.') + 1);
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
                                    objectMap.put("associations", TableAssociationParser.parse(
                                            table.getEntityName(), table.getComment()));
                                    objectMap.put("treeCascadeDelete",
                                            TableAssociationParser.treeCascadeDelete(table.getComment()));
                                    objectMap.put("tableDescription",
                                            TableAssociationParser.description(table.getComment()));
                                })
                                .customMap(MapUtil.<String, Object>builder()
                                        .put("context", meta.getContext())
                                        .put("boundedContextName", boundedContextName)
                                        .put("controllerPrefix", meta.getControllerPrefix())
                                        .put("package", meta.getPackageName())
                                        .put("serviceRootPackageName", serviceRootPackageName(meta))
                                        .put("entityPackageName", basePackage + ".domain.model")
                                        .put("criteriaPackageName", basePackage + ".application.criteria")
                                        .put("mapperPackageName", basePackage + ".infrastructure.persistence.mybatisplus")
                                        .put("servicePackageName", basePackage + ".application.service")
                                        .put("tablePrefix", meta.getTablePrefix())

                                        .put("idEntity", staticModels.get("vip.isass.framework.nocode.entity.IIdEntity"))
                                        .put("parentIdEntity", staticModels.get("vip.isass.framework.nocode.entity.IParentIdEntity"))
                                        .put("logicDeleteEntity", staticModels.get("vip.isass.framework.nocode.entity.ILogicDeleteEntity"))
                                        .put("tenantEntity", staticModels.get("vip.isass.framework.nocode.entity.ITenantEntity"))
                                        .put("traceEntity", staticModels.get("vip.isass.framework.nocode.entity.ITraceEntity"))
                                        .put("versionEntity", staticModels.get("vip.isass.framework.nocode.entity.IVersionEntity"))
                                        .build()
                                )
                                .customFile(CollUtil.newArrayList(
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/entity.java.ftl")
                                                        .packageName("domain.model")
                                                        .fileName(".java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isEntityFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/criteria.java.ftl")
                                                        .packageName("application.criteria")
                                                        .fileName("Criteria.java")
                                                        .formatNameFunction(tableInfo -> tableInfo.getEntityName()),
                                                meta.isCriteriaFileOverride()),
                                        customFile(new CustomFile.Builder()
                                                        .templatePath("/templates/nocode/IService.java.ftl")
                                                        .packageName("application.service")
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
        String boundedContextName = meta.getContext().substring(meta.getContext().lastIndexOf('.') + 1);
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
                                .customMap(MapUtil.<String, Object>builder()
                                        .put("context", meta.getContext())
                                        .put("boundedContextName", boundedContextName)
                                        .put("controllerPrefix", meta.getControllerPrefix())
                                        .put("package", meta.getPackageName())
                                        .put("serviceRootPackageName", serviceRootPackageName(meta))
                                        .put("entityPackageName", basePackage + ".domain.model")
                                        .put("criteriaPackageName", basePackage + ".application.criteria")
                                        .put("repositoryPackageName", basePackage + ".domain.repository")
                                        .put("mapperPackageName", basePackage + ".infrastructure.persistence.mybatisplus")
                                        .put("servicePackageName", basePackage + ".application.service")
                                        .put("tablePrefix", meta.getTablePrefix())

                                        .put("idEntity", staticModels.get("vip.isass.framework.nocode.entity.IIdEntity"))
                                        .put("parentIdEntity", staticModels.get("vip.isass.framework.nocode.entity.IParentIdEntity"))
                                        .put("logicDeleteEntity", staticModels.get("vip.isass.framework.nocode.entity.ILogicDeleteEntity"))
                                        .put("tenantEntity", staticModels.get("vip.isass.framework.nocode.entity.ITenantEntity"))
                                        .put("traceEntity", staticModels.get("vip.isass.framework.nocode.entity.ITraceEntity"))
                                        .put("versionEntity", staticModels.get("vip.isass.framework.nocode.entity.IVersionEntity"))
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
                                                        .packageName("application.service")
                                                        .fileName("ApplicationService.java")
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
        int separator = context.lastIndexOf('.');
        String serviceContext = separator < 0 ? context : context.substring(0, separator);
        return meta.getPackageName() + "." + serviceContext;
    }


}
