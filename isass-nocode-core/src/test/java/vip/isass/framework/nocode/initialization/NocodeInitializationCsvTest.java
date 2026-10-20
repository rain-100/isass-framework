// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import vip.isass.framework.common.criteria.field.IIdCriteria;
import vip.isass.framework.common.criteria.impl.type.FullTypeCriteria;
import vip.isass.framework.common.entity.IIdEntity;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.entrypoint.annotation.ApiDoc;
import vip.isass.framework.entrypoint.annotation.EntrypointInfo;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.service.ILocalCrudService;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NocodeInitializationCsvTest {

    @Test
    void writeOnlyCredentialsCanBeImportedButNeverExportedInCsv() throws Exception {
        var entity = new CsvEntity();
        entity.apiKey = "test-key-never-export";
        String csv = new String(NocodeInitializationCsv.write("csvSample", CsvEntity.class, List.of(entity)), StandardCharsets.UTF_8);
        assertThat(csv).contains("apiKey").doesNotContain("test-key-never-export");
        assertThat(read("apiKey\nAPI Key\ntest-input-key\n").get("csvSample").getFirst())
                .containsEntry("apiKey", "test-input-key");
    }

    @Test
    void readsUtf8BomQuotedCommasQuotesNewlinesJsonAndSparseRecordsWithoutLosingPrecision() throws Exception {
        String csv = "\uFEFFid,name,audiences,options,enabled\r\n"
                + "主键,名称,受众,配置,启用\r\n"
                + "2081264465596485633,\"他说：\"\"选题,创作\"\"\n第二行\",\"[\"\"青年\"\",\"\"职场人\"\"]\",\"{\"\"count\"\":2}\",TRUE\r\n"
                + ",,,,\r\n0002,[],[],{},FALSE\r\n";
        var rows = read(csv).get("csvSample");
        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst()).containsEntry("id", "2081264465596485633")
                .containsEntry("name", "他说：\"选题,创作\"\n第二行")
                .containsEntry("audiences", List.of("青年", "职场人"))
                .containsEntry("options", Map.of("count", 2)).containsEntry("enabled", "TRUE");
        assertThat(rows.get(1)).containsEntry("id", "0002").containsEntry("name", "[]")
                .containsEntry("audiences", List.of()).containsEntry("options", Map.of());
    }

    @Test
    void preservesLiteralStringsAndDistinguishesAbsentNullEmptyAndLegacyJson() throws Exception {
        String csv = "name,audiences,options,detail\n名称,受众,配置,详情\n"
                + "null,\"json:[\"\"青年\"\"]\",json:null,\n"
                + "\"json:\"\"\"\"\",[],{},\"{\"\"name\"\":\"\"人物\"\"}\"\n"
                + "json:[],[],{},\n";
        var rows = read(csv).get("csvSample");
        assertThat(rows.getFirst()).containsEntry("name", "null")
                .containsEntry("audiences", List.of("青年")).containsEntry("options", null)
                .doesNotContainKey("detail");
        assertThat(rows.get(1)).containsEntry("name", "").containsEntry("detail", Map.of("name", "人物"));
        assertThat(rows.get(2)).containsEntry("name", "json:[]");
    }

    @Test
    void exportsTheSameFieldsAndRemarksAsTheExcelTemplateAndRoundTripsAllValues() throws Exception {
        var entity = new CsvEntity();
        entity.id = 2081264465596485633L;
        entity.name = "你好,\"朋友\"\n下一行";
        entity.audiences = List.of("青年", "职场人");
        entity.options = Map.of("count", 2);
        entity.detail = new Detail();
        entity.detail.name = "人物";
        entity.tags = new String[]{"剧情", "讲述"};
        entity.payload = List.of(Map.of("name", "元素"));
        entity.state = State.OPEN;
        entity.enabled = true;
        byte[] bytes = NocodeInitializationCsv.write("csvSample", CsvEntity.class, List.of(entity));
        assertThat(new String(bytes, StandardCharsets.UTF_8)).startsWith("\uFEFFid,name");
        try (var parser = CSVFormat.RFC4180.parse(new StringReader(new String(bytes, StandardCharsets.UTF_8).substring(1)))) {
            var records = parser.getRecords();
            assertThat(records).hasSize(3);
            assertThat(records.getFirst().toList()).doesNotContain("createUserId", "modifyTime")
                    .contains("inherited", "tenantId", "version", "deleteFlag");
            assertThat(records.get(1).get(2)).contains("受众", "直接填写", "无需 json: 前缀");
        }
        var row = NocodeInitializationCsv.read(new ByteArrayInputStream(bytes), "测试数据--csvSample.csv",
                ignored -> CsvEntity.class).get("csvSample").getFirst();
        var restored = JsonUtil.convertValue(row, CsvEntity.class);
        assertThat(restored.id).isEqualTo(entity.id);
        assertThat(restored.name).isEqualTo(entity.name);
        assertThat(restored.audiences).isEqualTo(entity.audiences);
        assertThat(restored.options).isEqualTo(entity.options);
        assertThat(restored.detail.name).isEqualTo("人物");
        assertThat(restored.tags).containsExactly("剧情", "讲述");
        assertThat(restored.state).isEqualTo(State.OPEN);
        assertThat(restored.enabled).isTrue();
        assertThat(row).containsEntry("inherited", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "json:null", "json:\"\"", "json:[]", "=SUM(1,2)", "+cmd", "-cmd", "@name", "\t=cmd"})
    void escapesReservedAndSpreadsheetFormulaLikeStringsWithoutChangingTheirValue(String text) throws Exception {
        var entity = new CsvEntity();
        entity.name = text;
        byte[] bytes = NocodeInitializationCsv.write("csvSample", CsvEntity.class, List.of(entity));
        var row = NocodeInitializationCsv.read(new ByteArrayInputStream(bytes), "csvSample.csv",
                ignored -> CsvEntity.class).get("csvSample").getFirst();
        assertThat(row).containsEntry("name", text);
    }

    @ParameterizedTest
    @ValueSource(strings = {"csvSample.csv", "业务数据--csvSample.csv", "业务--分组--csvSample.CSV", "C:\\fakepath\\业务--csvSample.csv"})
    void resolvesTheResourceFromTheLastSeparatorOrTheWholeFilename(String filename) {
        assertThat(NocodeInitializationCsv.entityName(filename)).isEqualTo("csvSample");
    }

    @ParameterizedTest
    @ValueSource(strings = {"业务--.csv", "--csvSample.csv", "业务--csv-sample.csv", "业务--csvSample.txt"})
    void rejectsInvalidResourceNames(String filename) {
        assertThatThrownBy(() -> NocodeInitializationCsv.entityName(filename)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "id,id\n说明,说明\n1,2", "id,\n说明,说明\n1,2", "id\n说明\n1,orphan", "audiences\n说明\n[invalid]", "name\n说明\n\"unterminated"})
    void rejectsMalformedCsvBeforeImportAndIncludesFileContext(String csv) {
        assertThatThrownBy(() -> read(csv)).hasMessageContaining("测试--csvSample.csv");
    }

    @Test
    void parsesUsingRemoteApiMetadataAndRoutesThroughTheExistingImporter() throws Exception {
        var local = new NocodeInitializationDataService(List.of());
        var remote = mock(INocodeInitializationService.class);
        var definition = NocodeInitializationImporterTest.definition("csv-service", "csvSample");
        doReturn(CsvEntrypoint.class).when(definition).serviceInterface();
        var registry = mock(ServiceDefinitionRegistry.class);
        when(registry.all()).thenReturn(List.of(definition));
        when(remote.importData(eq("csv-service"), any()))
                .thenReturn(new NocodeInitializationDataService.ImportResult(1, 1, 0, Map.of()));
        var importer = new NocodeInitializationImporter(local, remote, registry);
        var document = importer.readDocument(input("audiences\n受众\n\"[\"\"青年\"\"]\"\n"), "测试--csvSample.csv");
        assertThat(importer.importData("entry-service", document).inserted()).isEqualTo(1);
        verify(remote).importData("csv-service", Map.of("csvSample", List.of(Map.of("audiences", List.of("青年")))));
    }

    private static Map<String, List<Map<String, Object>>> read(String csv) throws Exception {
        return NocodeInitializationCsv.read(input(csv), "测试--csvSample.csv", ignored -> CsvEntity.class);
    }

    private static ByteArrayInputStream input(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }

    static class CsvBase {
        public String inherited;
        public Long tenantId;
        public Long version;
        public Boolean deleteFlag;
        public Long createUserId;
        public Long modifyTime;
    }

    static class CsvEntity extends CsvBase implements IIdEntity<Long, CsvEntity> {
        public Long id;
        public String name;
        @ApiDoc(description = "受众") public List<String> audiences;
        public Map<String, Object> options;
        public Detail detail;
        public String[] tags;
        public Object payload;
        public State state;
        public Boolean enabled;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String apiKey;
        @Override public Long getId() { return id; }
        @Override public void setId(Long id) { this.id = id; }
        @Override public CsvEntity randomEntity() { return this; }
    }

    static class Detail {
        public String name;
    }

    enum State {
        OPEN;
        @JsonValue public String code() { return "open"; }
    }

    @EntrypointInfo(serviceName = "csv-service", contextName = "csv", resourceName = "csvSample")
    interface CsvEntrypoint extends ILocalCrudService<CsvEntity, CsvCriteria, Long> { }

    static final class CsvCriteria extends FullTypeCriteria<CsvEntity, CsvCriteria>
            implements IIdCriteria<Long, CsvEntity, CsvCriteria> { }
}
