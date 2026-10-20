// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vip.isass.framework.common.entity.EntityAssociation;
import vip.isass.framework.common.entity.IEntity;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.nocode.generator.association.EntityRelationDefinition;
import vip.isass.framework.nocode.generator.model.EntityFieldDefinition;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityAssociationTemplateTest {

    private static final String GENERATED_PACKAGE = "example.generated";

    @TempDir
    Path temporaryDirectory;

    @Test
    void generatedMembersSettersAndBuildersPreserveDeclaredTypesAndJsonOrder() throws Exception {
        String workSource = render("Work", List.of(
                EntityRelationDefinition.list("Work", "storyboardShots", "StoryboardShot", "id", "workId"),
                EntityRelationDefinition.many("Work", "aiTasks", "AiTask", "id", "workId"),
                EntityRelationDefinition.one("Work", "projectRule", "ProjectRule", "id", "id")), false);
        Map<String, String> sources = new HashMap<>();
        sources.put("Work", workSource);
        for (String entity : List.of("StoryboardShot", "AiTask", "ProjectRule")) {
            sources.put(entity, render(entity, List.of(), false));
        }
        sources.put("TreeNode", render("TreeNode", List.of(), true));

        try (URLClassLoader loader = compile(sources)) {
            Class<?> workType = loader.loadClass(GENERATED_PACKAGE + ".Work");
            assertThat(workType.getDeclaredField("storyboardShots").getType()).isEqualTo(List.class);
            assertThat(workType.getDeclaredField("storyboardShots").getGenericType().getTypeName())
                    .isEqualTo("java.util.List<" + GENERATED_PACKAGE + ".StoryboardShot>");
            assertThat(workType.getMethod("getStoryboardShots").getReturnType()).isEqualTo(List.class);
            assertThat(workType.getMethod("setStoryboardShots", List.class)).isNotNull();
            assertThat(workType.getDeclaredField("aiTasks").getType()).isEqualTo(Collection.class);
            assertThat(workType.getMethod("setAiTasks", Collection.class)).isNotNull();
            assertThat(workType.getDeclaredField("projectRule").getType().getSimpleName()).isEqualTo("ProjectRule");
            Object builder = workType.getMethod("builder").invoke(null);
            assertThat(builder.getClass().getMethod("storyboardShots", List.class)).isNotNull();
            assertThat(builder.getClass().getMethod("aiTasks", Collection.class)).isNotNull();

            Object work = JsonUtil.readValue("""
                    {"id":1,"storyboardShots":[{"id":20},{"id":10}],
                     "aiTasks":[{"id":30}],"projectRule":{"id":40}}
                    """, workType);
            Object restored = JsonUtil.readValue(JsonUtil.writeValue(work), workType);
            List<?> shots = (List<?>) workType.getMethod("getStoryboardShots").invoke(restored);
            assertThat(shots.stream().map(shot -> Long.class.cast(((IIdEntity<?, ?>) shot).getId())).toList())
                    .containsExactly(20L, 10L);
            assertThat(workType.getMethod("getAiTasks").invoke(restored)).isInstanceOf(Collection.class);
            EntityAssociation association = ((IEntity<?>) restored).associations().stream()
                    .filter(candidate -> candidate.property().equals("storyboardShots")).findFirst().orElseThrow();
            assertThat(association.kind()).isEqualTo(EntityAssociation.Kind.MANY);
            assertThat(association.localField()).isEqualTo("id");
            assertThat(association.targetField()).isEqualTo("workId");
            assertThat(loader.loadClass(GENERATED_PACKAGE + ".TreeNode").getDeclaredField("children").getType())
                    .isEqualTo(List.class);
        }
    }

    @Test
    void listOnlyRelationsDoNotImportOrGenerateCollectionMembers() throws Exception {
        String source = render("Work", List.of(EntityRelationDefinition.list(
                "Work", "storyboardShots", "StoryboardShot", "id", "workId")), false);

        assertThat(source).contains("private List<StoryboardShot> storyboardShots;",
                        "setStoryboardShots(List<StoryboardShot> storyboardShots)")
                .doesNotContain("import java.util.Collection;", "Collection<StoryboardShot>");
        assertThat(source.lines().filter(line -> line.equals("import java.util.List;")).count()).isEqualTo(1);
    }

    private String render(String entity, List<EntityRelationDefinition> definitions, boolean tree) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);
        configuration.setClassForTemplateLoading(getClass(), "/templates/nocode");
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(entity.startsWith("BusinessId")
                ? typedField("id", "id", "String", true) : field("id", "id", true));
        if (entity.startsWith("BusinessId")) {
            fields.add(typedField("create_user_id", "createUserId", "Long", false));
            fields.add(typedField("modify_user_id", "modifyUserId",
                    entity.equals("BusinessIdMismatch") ? "String" : "Long", false));
            fields.add(typedField("create_user_name", "createUserName", "String", false));
            fields.add(typedField("modify_user_name", "modifyUserName", "String", false));
            fields.add(typedField("create_time", "createTime", "Long", false));
            fields.add(typedField("modify_time", "modifyTime", "Long", false));
        }
        if (entity.equals("Snapshot")) {
            fields.add(Map.of("name", "selected_path", "propertyName", "selectedPath", "propertyType", "JsonNode",
                    "keyFlag", false, "comment", "已选路径", "metaInfo", Map.of("typeName", "JSON")));
            fields.add(Map.of("name", "choice", "propertyName", "choice", "propertyType", "Integer",
                    "keyFlag", false, "comment", "选择", "metaInfo", Map.of("typeName", "TINYINT")));
        }
        if (entity.equals("SecretConfig")) {
            fields.add(typedField("api_key", "apiKey", "String", false));
        }
        if (List.of("StoryboardShot", "AiTask").contains(entity)) {
            fields.add(field("work_id", "workId", false));
        }
        if (tree) fields.add(field("parent_id", "parentId", false));
        Map<String, Object> model = new HashMap<>();
        model.put("cfg", Map.of("entityPackageName", GENERATED_PACKAGE));
        model.put("entity", entity);
        model.put("author", "test");
        model.put("tableDescription", entity);
        String tableName = "test_creation_" + entity.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
        model.put("table", Map.of("name", tableName, "fields", fields));
        model.put("modelFields", entity.equals("Snapshot")
                ? Map.of("selectedPath", EntityFieldDefinition.type("selectedPath", "List<java.net.URI>"),
                        "choice", EntityFieldDefinition.type("choice", "java.time.DayOfWeek"))
                : entity.equals("SecretConfig")
                        ? Map.of("apiKey", EntityFieldDefinition.secret("apiKey")) : Map.of());
        model.put("tenantIsolation", true);
        model.put("treeCascadeDelete", false);
        model.put("associations", definitions.stream().map(EntityRelationDefinition::association).toList());
        model.put("associationImports", List.of());
        StringWriter output = new StringWriter();
        configuration.getTemplate("entity.java.ftl").process(model, output);
        return output.toString();
    }

    @Test
    void stringBusinessIdUsesIndependentLongAuditTypeAndCompiles() throws Exception {
        String source = render("BusinessId", List.of(), false);
        assertThat(source).contains("IIdEntity<String, BusinessId>", "ITraceEntity<Long, BusinessId>");
        try (URLClassLoader loader = compile(Map.of("BusinessId", source))) {
            Class<?> type = loader.loadClass(GENERATED_PACKAGE + ".BusinessId");
            assertThat(type.getMethod("getId").getReturnType()).isEqualTo(String.class);
            assertThat(type.getMethod("getCreateUserId").getReturnType()).isEqualTo(Long.class);
        }
    }

    @Test
    void mismatchedAuditUserTypesFailBeforeGeneratingInvalidJava() {
        assertThatThrownBy(() -> render("BusinessIdMismatch", List.of(), false))
                .isInstanceOf(TemplateException.class)
                .hasMessageContaining("create_user_id 与 modify_user_id 的 Java 类型必须一致");
    }

    private Map<String, Object> typedField(String column, String property, String type, boolean primaryKey) {
        return Map.of("name", column, "propertyName", property, "propertyType", type,
                "keyFlag", primaryKey, "comment", property, "metaInfo", Map.of("typeName", type));
    }

    private Map<String, Object> field(String column, String property, boolean primaryKey) {
        return Map.of("name", column, "propertyName", property, "propertyType", "Long",
                "keyFlag", primaryKey, "comment", property, "metaInfo", Map.of("typeName", "BIGINT"));
    }

    @Test
    void qualifiedJsonElementTypesUseExplicitImports() throws Exception {
        String source = render("Snapshot", List.of(), false);
        assertThat(source).contains("import java.net.URI;", "private List<URI> selectedPath;",
                "setSelectedPath(List<URI> selectedPath)", "import java.time.DayOfWeek;",
                "private DayOfWeek choice;").doesNotContain("List<java.net.URI>", "setChoice(randomInteger())");
        try (URLClassLoader loader = compile(Map.of("Snapshot", source))) {
            assertThat(loader.loadClass(GENERATED_PACKAGE + ".Snapshot").getDeclaredField("selectedPath")
                    .getGenericType().getTypeName()).isEqualTo("java.util.List<java.net.URI>");
        }
    }

    private URLClassLoader compile(Map<String, String> sources) throws Exception {
        List<Path> files = new ArrayList<>();
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = temporaryDirectory.resolve(source.getKey() + ".java");
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            files.add(file);
        }
        Path classes = Files.createDirectory(temporaryDirectory.resolve("classes"));
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("生成结果编译测试需要 JDK").isNotNull();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                diagnostics, null, StandardCharsets.UTF_8)) {
            boolean success = compiler.getTask(null, fileManager, diagnostics, List.of(
                    "-classpath", classpath, "-processorpath", classpath,
                    "-processor", "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
                    "-d", classes.toString()), null, fileManager.getJavaFileObjectsFromPaths(files)).call();
            assertThat(success).as("生成实体应能通过 Lombok 编译：%s", diagnostics.getDiagnostics()).isTrue();
        }
        return new URLClassLoader(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }

    @Test
    void secretFieldsAcceptJsonInputButNeverLeaveThroughJsonOrToString() throws Exception {
        String source = render("SecretConfig", List.of(), false);
        try (URLClassLoader loader = compile(Map.of("SecretConfig", source))) {
            Class<?> type = loader.loadClass(GENERATED_PACKAGE + ".SecretConfig");
            Object config = JsonUtil.readValue("{\"id\":1,\"apiKey\":\"test-key-never-expose\"}", type);
            assertThat(type.getMethod("getApiKey").invoke(config)).isEqualTo("test-key-never-expose");
            assertThat(((IEntity<?>) config).isPropertyPresent("apiKey")).isTrue();
            assertThat(JsonUtil.writeValue(config)).doesNotContain("apiKey", "test-key-never-expose");
            assertThat(config.toString()).doesNotContain("apiKey", "test-key-never-expose");
        }
    }
}
