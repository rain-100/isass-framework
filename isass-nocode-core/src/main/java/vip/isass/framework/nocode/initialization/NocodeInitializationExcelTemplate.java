// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds an initialization workbook from the registered API entity types without reading business data.
 */
final class NocodeInitializationExcelTemplate {

    private NocodeInitializationExcelTemplate() {
    }

    static byte[] create(Map<String, Class<?>> entityTypes) throws IOException {
        Set<String> sheetNames = new LinkedHashSet<>();
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle commentStyle = commentStyle(workbook);
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(workbook.createDataFormat().getFormat("@"));
            for (var entry : entityTypes.entrySet()) {
                String name = entry.getKey();
                if (name.length() > 31 || !name.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")
                        || !sheetNames.add(name.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("实体名不适合作为 Excel sheet 名或存在名称冲突: " + name);
                }
                WorkbookUtil.validateSheetName(name);
                List<Field> fields = NocodeInitializationTableSupport.templateFields(entry.getValue());
                var sheet = workbook.createSheet(name);
                sheet.setDisplayGridlines(true);
                sheet.createFreezePane(0, 2);
                var header = sheet.createRow(0);
                var comments = sheet.createRow(1);
                sheet.createRow(2);
                header.setHeightInPoints(28);
                comments.setHeightInPoints(72);
                for (int column = 0; column < fields.size(); column++) {
                    Field field = fields.get(column);
                    var headerCell = header.createCell(column);
                    headerCell.setCellValue(field.getName());
                    headerCell.setCellStyle(headerStyle);
                    var commentCell = comments.createCell(column);
                    commentCell.setCellValue(NocodeInitializationTableSupport.description(field));
                    commentCell.setCellStyle(commentStyle);
                    sheet.setColumnWidth(column, 28 * 256);
                    if (!field.getType().isPrimitive() && !Number.class.isAssignableFrom(field.getType())
                            || field.getType() == Long.class || field.getType() == long.class
                            || field.getType() == BigInteger.class) {
                        sheet.setDefaultColumnStyle(column, textStyle);
                    }
                }
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        var font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        borders(style);
        return style;
    }

    private static CellStyle commentStyle(XSSFWorkbook workbook) {
        var style = workbook.createCellStyle();
        style.setFillForegroundColor(rgb("EEE9F6"));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        var font = workbook.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 11);
        font.setColor(rgb("393448"));
        style.setFont(font);
        borders(style);
        return style;
    }

    private static XSSFColor rgb(String hex) {
        return new XSSFColor(HexFormat.of().parseHex(hex), new DefaultIndexedColorMap());
    }

    private static void borders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        short color = IndexedColors.GREY_25_PERCENT.getIndex();
        style.setTopBorderColor(color);
        style.setRightBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
    }

}
