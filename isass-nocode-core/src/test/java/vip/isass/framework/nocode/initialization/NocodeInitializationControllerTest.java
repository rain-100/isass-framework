// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vip.isass.framework.common.support.JsonUtil;
import vip.isass.framework.database.core.repository.IRepository;
import vip.isass.framework.entrypoint.registry.ServiceDefinitionRegistry;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.Sample;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.SampleCriteria;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataServiceTest.SampleService;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NocodeInitializationControllerTest {

    private IRepository<Sample, SampleCriteria> repository;
    private INocodeInitializationService remote;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(IRepository.class);
        when(repository.findByCriteria(any())).thenReturn(List.of());
        var local = new NocodeInitializationDataService(List.of(new SampleService(repository)));
        remote = mock(INocodeInitializationService.class);
        var registry = mock(ServiceDefinitionRegistry.class);
        var definition = NocodeInitializationImporterTest.definition("remote-service", "remoteSample");
        doReturn(NocodeInitializationDataServiceTest.SampleEntrypoint.class).when(definition).serviceInterface();
        when(registry.all()).thenReturn(List.of(definition));
        var importer = new NocodeInitializationImporter(local, remote, registry);
        mvc = MockMvcBuilders.standaloneSetup(new NocodeInitializationController(local, importer)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "xlsx"})
    void uploadedFilesUseTheSameRoutingAndFailureAggregation(String extension) throws Exception {
        var file = new MockMultipartFile("file", "data." + extension,
                "application/octet-stream", contents(extension));

        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.inserted").value(1))
                .andExpect(jsonPath("$.data.failures.unknown").exists());

        verify(repository).add(any(Sample.class));
        verifyNoInteractions(remote);
    }

    @Test
    void jsonRequestsAutomaticallyRouteEntitiesToTheirOwningService() throws Exception {
        when(remote.importData(eq("remote-service"), any()))
                .thenReturn(new ImportResult(1, 1, 0, Map.of()));

        mvc.perform(post("/entry-service/nocode/system/initialization/importData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remoteSample\":[{\"id\":\"2081264465596485633\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.inserted").value(1));

        verify(remote).importData("remote-service",
                Map.of("remoteSample", List.of(Map.of("id", "2081264465596485633"))));
        verifyNoInteractions(repository);
    }

    @Test
    void addressedRemoteTargetDoesNotForwardBackToItself() throws Exception {
        mvc.perform(post("/remote-service/nocode/system/initialization/importData")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"remoteSample\":[{\"id\":\"1\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.failures.remoteSample").exists());

        verifyNoInteractions(remote, repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "unsupported", "csv", "corruptXlsx", "invalidJson"})
    void rejectsInvalidUploadsBeforeAnyWrites(String kind) throws Exception {
        String filename = switch (kind) {
            case "csv" -> "业务--sample.csv";
            case "unsupported" -> "data.txt";
            case "corruptXlsx" -> "data.xlsx";
            default -> "data.json";
        };
        byte[] bytes = (kind.equals("empty") ? "" : kind.equals("csv")
                ? "id,id\n主键,主键\n1,2" : "invalid").getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", filename, "application/octet-stream", bytes);

        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(remote, repository);
    }

    @Test
    void missingFileReturnsBadRequest() throws Exception {
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(remote, repository);
    }

    @Test
    void listsRegisteredModelsWithServiceAndContextWithoutReadingOrRequestingBusinessData() throws Exception {
        mvc.perform(get("/entry-service/nocode/system/initialization/entities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].serviceName").value("remote-service"))
                .andExpect(jsonPath("$.data[0].contextName").value("sample"))
                .andExpect(jsonPath("$.data[0].entity").value("remoteSample"))
                .andExpect(jsonPath("$.data[1].serviceName").value("sample-service"))
                .andExpect(jsonPath("$.data[1].contextName").value("sample"))
                .andExpect(jsonPath("$.data[1].entity").value("sample"));
        verifyNoInteractions(repository, remote);
    }

    @Test
    void exportsRemoteCsvByItsOwningServiceAndConvertsWireRowsBackToTheApiEntity() throws Exception {
        when(remote.exportData(eq("remote-service"), any())).thenReturn(Map.of("remoteSample", List.of(
                Map.of("id", "2081264465596485633", "name", "远程,数据"))));
        var response = mvc.perform(get("/entry-service/nocode/system/initialization/exportData")
                        .param("entities", "remoteSample").param("format", "csv"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains("2081264465596485633", "\"远程,数据\"");
        verify(remote).exportData("remote-service", Set.of("remoteSample"));
        verifyNoInteractions(repository);
    }

    @Test
    void addressedExportTargetWithoutALocalImplementationDoesNotForwardToItself() throws Exception {
        mvc.perform(get("/remote-service/nocode/system/initialization/exportData")
                        .param("entities", "remoteSample").param("format", "csv"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("导出目标服务未部署本地实体: remote-service"));
        verifyNoInteractions(repository, remote);
    }

    private byte[] contents(String extension) throws Exception {
        if (extension.equals("json")) {
            Map<String, List<Map<String, Object>>> document = new LinkedHashMap<>();
            document.put("unknown", List.of(Map.of("id", "1")));
            document.put("sample", List.of(Map.of("id", "2081264465596485633", "name", "valid")));
            return JsonUtil.writeValueAsBytes(document);
        }
        try (var workbook = new XSSFWorkbook()) {
            for (String entity : List.of("unknown", "sample")) {
                var sheet = workbook.createSheet(entity);
                sheet.createRow(0).createCell(0).setCellValue("id");
                sheet.createRow(1).createCell(0).setCellValue("主键");
                sheet.createRow(2).createCell(0).setCellValue("2081264465596485633");
            }
            var output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    @Test
    void downloadsSheetsInRequestOrderForLocalAndRemoteApiTypesWithoutReadingBusinessData() throws Exception {
        var response = mvc.perform(get("/entry-service/nocode/system/initialization/downloadTemplate")
                        .param("entities", "remoteSample,sample,remoteSample"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition")).contains("attachment", "initialization-template.xlsx");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetName(0)).isEqualTo("remoteSample");
            assertThat(workbook.getSheetName(1)).isEqualTo("sample");
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("id");
            assertThat(workbook.getSheetAt(0).getColumnStyle(0).getDataFormatString()).isEqualTo("@");
        }
        verifyNoInteractions(repository, remote);
    }

    @Test
    void downloadedTemplateCanBeFilledAndUploadedWithoutImportingTheCommentRow() throws Exception {
        var response = mvc.perform(get("/entry-service/nocode/system/initialization/downloadTemplate")
                        .param("entities", "sample"))
                .andExpect(status().isOk()).andReturn().getResponse();
        byte[] bytes;
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var row = workbook.getSheetAt(0).getRow(2);
            row.createCell(0).setCellValue("2081264465596485633");
            row.createCell(1).setCellValue("valid");
            var output = new ByteArrayOutputStream();
            workbook.write(output);
            bytes = output.toByteArray();
        }
        var file = new MockMultipartFile("file", "data.xlsx", "application/octet-stream", bytes);
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.inserted").value(1));
    }

    @Test
    void csvTemplateUsesTwoHeaderRecordsAndTheDownloadedFileCanBeUploaded() throws Exception {
        var response = mvc.perform(get("/entry-service/nocode/system/initialization/downloadTemplate")
                        .param("entities", "sample").param("format", "csv"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition")).contains("sample.csv");
        String template = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        assertThat(template).isEqualTo("\uFEFFid,name\r\nid,name\r\n");
        var empty = new MockMultipartFile("file", "模版--sample.csv", "text/csv", response.getContentAsByteArray());
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(empty))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        verifyNoInteractions(repository, remote);

        var file = new MockMultipartFile("file", "业务--sample.csv", "text/csv",
                (template + "2081264465596485633,\"你好,朋友\"\r\n").getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.inserted").value(1));
        verify(repository).add(any(Sample.class));
    }

    @Test
    void exportsCsvDataThatRoundTripsThroughTheSameUploadInterface() throws Exception {
        var sample = new Sample();
        sample.setId(2081264465596485633L);
        sample.setName("你好,\"朋友\"\n第二行");
        when(repository.findAll()).thenReturn(List.of(sample));
        var response = mvc.perform(get("/sample-service/nocode/system/initialization/exportData")
                        .param("entities", "sample").param("format", "csv"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentType()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeader("Content-Disposition")).contains("sample.csv");
        var file = new MockMultipartFile("file", "导出--sample.csv", "text/csv", response.getContentAsByteArray());
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.inserted").value(1));
        var captor = ArgumentCaptor.forClass(Sample.class);
        verify(repository).add(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(sample.getId());
        assertThat(captor.getValue().getName()).isEqualTo(sample.getName());
    }

    @Test
    void csvDownloadsRejectMultipleEntitiesAndUnsupportedFormatsBeforeReadingData() throws Exception {
        for (String path : List.of("downloadTemplate", "exportData")) {
            mvc.perform(get("/sample-service/nocode/system/initialization/" + path)
                            .param("entities", "sample,remoteSample").param("format", "csv"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(
                            "一个 CSV 文件只能对应一个实体，请每次传入一个实体资源名"));
            mvc.perform(get("/sample-service/nocode/system/initialization/" + path)
                            .param("entities", "sample").param("format", "invalid"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository, remote);
    }

    @Test
    void aLateCsvSyntaxErrorPreventsAllWrites() throws Exception {
        var file = new MockMultipartFile("file", "业务--sample.csv", "text/csv",
                "id,name\n主键,名称\n1,valid\n2,\"unterminated".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/entry-service/nocode/system/initialization/importFile").file(file))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(repository, remote);
    }

    @Test
    void invalidEntityNamesReturnJsonErrorsInsteadOfAnInvalidWorkbook() throws Exception {
        for (String entity : List.of("", "unknown")) {
            mvc.perform(get("/entry-service/nocode/system/initialization/downloadTemplate").param("entities", entity))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        mvc.perform(get("/entry-service/nocode/system/initialization/downloadTemplate"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, remote);
    }
}
