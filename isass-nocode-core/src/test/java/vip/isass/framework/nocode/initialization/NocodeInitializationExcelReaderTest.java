// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NocodeInitializationExcelReaderTest {

    private final NocodeInitializationDataService service = new NocodeInitializationDataService(List.of());

    @Test
    void readsSheetsInOrderPreservingTextIdsTypesJsonAndSparseRows() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            Sheet types = workbook.createSheet("dict");
            header(types, "id", "name", "orderNum", "enableFlag", "children", "remark");
            var row = types.createRow(2);
            row.createCell(0).setCellValue("2081264465596485633");
            row.createCell(1).setCellValue("妙思目标受众");
            row.createCell(2).setCellValue(1);
            row.createCell(3).setCellValue(true);
            row.createCell(4).setCellValue("json:[{\"name\":\"学生\"}]");
            types.createRow(3).createCell(0).setCellValue("");
            types.createRow(5).createCell(0).setCellValue("0002");
            Sheet items = workbook.createSheet("directory");
            header(items, "id", "enableFlag", "remark", "name");
            items.createRow(2).createCell(0).setCellValue("3");
            items.getRow(2).createCell(1).setCellValue(false);
            items.getRow(2).createCell(2).setCellValue("json:null");
            items.getRow(2).createCell(3).setCellValue("json:\"\"");

            var document = service.readDocument(input(workbook), "data.XLSX");
            assertThat(document.keySet()).containsExactly("dict", "directory");
            assertThat(document.get("dict")).hasSize(2);
            assertThat(document.get("dict").getFirst())
                    .containsEntry("id", "2081264465596485633")
                    .containsEntry("name", "妙思目标受众")
                    .containsEntry("orderNum", BigDecimal.ONE)
                    .containsEntry("enableFlag", true)
                    .containsEntry("children", List.of(Map.of("name", "学生")))
                    .doesNotContainKey("remark");
            assertThat(document.get("dict").get(1)).containsExactly(Map.entry("id", "0002"));
            assertThat(document.get("directory").getFirst())
                    .containsEntry("enableFlag", false)
                    .containsEntry("remark", null)
                    .containsEntry("name", "");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"formula", "error", "largeNumber", "date", "invalidJson"})
    void rejectsUnsafeValuesWithSheetAndCellLocation(String kind) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("dict");
            header(sheet, "id");
            Cell cell = sheet.createRow(2).createCell(0);
            switch (kind) {
                case "formula" -> cell.setCellFormula("1+1");
                case "error" -> cell.setCellErrorValue(FormulaError.DIV0.getCode());
                case "largeNumber" -> cell.setCellValue(2081264465596485633D);
                case "date" -> {
                    cell.setCellValue(45000);
                    var style = workbook.createCellStyle();
                    style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
                    cell.setCellStyle(style);
                }
                case "invalidJson" -> cell.setCellValue("json:[invalid]");
                default -> throw new IllegalArgumentException(kind);
            }
            var input = input(workbook);
            assertThatThrownBy(() -> service.readDocument(input, "data.xlsx"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dict!A3");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicateHeader", "blankHeader", "missingHeader", "extraColumn", "merged"})
    void rejectsMalformedTablesInsteadOfLosingData(String kind) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("sample");
            header(sheet, "id", "name");
            switch (kind) {
                case "duplicateHeader" -> sheet.getRow(0).getCell(1).setCellValue("id");
                case "blankHeader" -> sheet.getRow(0).getCell(0).setCellValue("");
                case "missingHeader" -> sheet.removeRow(sheet.getRow(0));
                case "extraColumn" -> sheet.createRow(2).createCell(2).setCellValue("orphan");
                case "merged" -> sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 1));
                default -> throw new IllegalArgumentException(kind);
            }
            var input = input(workbook);
            assertThatThrownBy(() -> service.readDocument(input, "data.xlsx"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sample");
        }
    }

    @Test
    void preservesJsonEntryPointAndRejectsUnsupportedFormats() throws Exception {
        String json = "{\"sample\":[{\"id\":\"2081264465596485633\"}]}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        assertThat(service.readDocument(new ByteArrayInputStream(bytes), "data.json"))
                .isEqualTo(service.readDocument(new ByteArrayInputStream(bytes)));
        assertThatThrownBy(() -> service.readDocument(new ByteArrayInputStream(bytes), "data.xml"))
                .hasMessageContaining(".json、.xlsx 或 .csv");
        assertThat(NocodeInitializationDataService.supportsDocument("~$data.xlsx")).isFalse();
        assertThat(NocodeInitializationDataService.supportsDocument(null)).isFalse();
        assertThat(new NocodeInitializationProperties().getLocation()).isEqualTo("classpath*:init/**/*");
    }

    @Test
    void parsesJsonByFieldTypeAndKeepsStringContentVerbatim() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("sample");
            header(sheet, "audiences", "options", "detail", "tags", "name", "payload");
            var row = sheet.createRow(2);
            row.createCell(0).setCellValue("[\"青年\",\"职场人\"]");
            row.createCell(1).setCellValue("{\"count\":2}");
            row.createCell(2).setCellValue("{\"name\":\"人物\"}");
            row.createCell(3).setCellValue("[\"剧情\"]");
            row.createCell(4).setCellValue("[\"这只是正文\"]");
            row.createCell(5).setCellValue("{\"items\":[1,2]}");
            var document = service.readDocument(input(workbook), "sample.xlsx",
                    ignored -> NocodeInitializationCsvTest.CsvEntity.class);
            assertThat(document.get("sample").getFirst())
                    .containsEntry("audiences", List.of("青年", "职场人"))
                    .containsEntry("options", Map.of("count", 2))
                    .containsEntry("detail", Map.of("name", "人物"))
                    .containsEntry("tags", List.of("剧情"))
                    .containsEntry("name", "[\"这只是正文\"]")
                    .containsEntry("payload", Map.of("items", List.of(1, 2)));
            row.getCell(0).setCellValue("[invalid]");
            var invalid = input(workbook);
            assertThatThrownBy(() -> service.readDocument(invalid, "sample.xlsx",
                    ignored -> NocodeInitializationCsvTest.CsvEntity.class))
                    .hasMessageContaining("sample!A3").hasMessageContaining("有效 JSON");
        }
    }

    private static void header(Sheet sheet, String... fields) {
        var row = sheet.createRow(0);
        var comments = sheet.createRow(1);
        for (int column = 0; column < fields.length; column++) {
            row.createCell(column).setCellValue(fields[column]);
            comments.createCell(column).setCellValue(fields[column]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "text", "formula"})
    void skipsTheSecondRowWithoutInspectingItsContents(String kind) throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("sample");
            header(sheet, "id");
            switch (kind) {
                case "missing" -> sheet.removeRow(sheet.getRow(1));
                case "text" -> sheet.getRow(1).getCell(0).setCellValue("主键");
                case "formula" -> sheet.getRow(1).getCell(0).setCellFormula("1+1");
                default -> throw new IllegalArgumentException(kind);
            }
            sheet.createRow(2).createCell(0).setCellValue("2081264465596485633");
            var document = service.readDocument(input(workbook), "data.xlsx");
            assertThat(document.get("sample"))
                    .containsExactly(Map.of("id", "2081264465596485633"));
        }
    }

    private static ByteArrayInputStream input(XSSFWorkbook workbook) throws Exception {
        var output = new ByteArrayOutputStream();
        workbook.write(output);
        return new ByteArrayInputStream(output.toByteArray());
    }
}
