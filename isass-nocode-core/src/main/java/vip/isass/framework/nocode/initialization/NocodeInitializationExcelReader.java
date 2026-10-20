// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Converts Excel initialization data to the same document structure as JSON imports.
 */
final class NocodeInitializationExcelReader {

    private NocodeInitializationExcelReader() {
    }

    static Map<String, List<Map<String, Object>>> read(InputStream input,
                                                     Function<String, Class<?>> entityTypes) throws IOException {
        Map<String, List<Map<String, Object>>> document = new LinkedHashMap<>();
        try (var workbook = new XSSFWorkbook(input)) {
            for (Sheet sheet : workbook) {
                if (!sheet.getSheetName().matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
                    throw new IllegalArgumentException("初始化 sheet 名必须是实体资源名: " + sheet.getSheetName());
                }
                if (sheet.getNumMergedRegions() > 0) {
                    throw new IllegalArgumentException("初始化 sheet 不允许合并单元格: " + sheet.getSheetName());
                }
                document.put(sheet.getSheetName(), readSheet(sheet, entityTypes.apply(sheet.getSheetName())));
            }
        }
        return document;
    }

    private static List<Map<String, Object>> readSheet(Sheet sheet, Class<?> entityType) {
        Map<String, Field> metadata = NocodeInitializationTableSupport.fields(entityType);
        Row header = sheet.getRow(0);
        if (header == null || header.getLastCellNum() <= 0) {
            throw new IllegalArgumentException("初始化 sheet 第一行必须填写字段名: " + sheet.getSheetName());
        }
        List<String> fields = new ArrayList<>();
        Set<String> uniqueFields = new LinkedHashSet<>();
        for (int column = 0; column < header.getLastCellNum(); column++) {
            Cell cell = header.getCell(column);
            if (isBlank(cell) || cell.getCellType() != CellType.STRING) {
                throw invalid(sheet, 0, column, "字段名必须是非空文本");
            }
            String field = cell.getStringCellValue().trim();
            if (!field.matches("[a-zA-Z_$][a-zA-Z0-9_$]*") || !uniqueFields.add(field)) {
                throw invalid(sheet, 0, column, "字段名无效或重复: " + field);
            }
            fields.add(field);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Row row : sheet) {
            if (row.getRowNum() < 2) {
                continue;
            }
            Map<String, Object> values = new LinkedHashMap<>();
            for (Cell cell : row) {
                if (isBlank(cell)) {
                    continue;
                }
                int column = cell.getColumnIndex();
                if (column >= fields.size()) {
                    throw invalid(sheet, row.getRowNum(), column, "数据列缺少对应字段名");
                }
                String field = fields.get(column);
                values.put(field, readCell(cell, metadata.get(field)));
            }
            if (!values.isEmpty()) {
                rows.add(values);
            }
        }
        return rows;
    }

    private static boolean isBlank(Cell cell) {
        return cell == null || cell.getCellType() == CellType.BLANK
                || cell.getCellType() == CellType.STRING && cell.getStringCellValue().isEmpty();
    }

    private static Object readCell(Cell cell, Field field) {
        return switch (cell.getCellType()) {
            case STRING -> readString(cell, field);
            case BOOLEAN -> cell.getBooleanCellValue();
            case NUMERIC -> readNumber(cell);
            case FORMULA -> throw invalid(cell, "不允许公式，请粘贴为值");
            case ERROR -> throw invalid(cell, "不允许 Excel 错误值");
            default -> throw invalid(cell, "不支持的单元格类型: " + cell.getCellType());
        };
    }

    private static Object readString(Cell cell, Field field) {
        try {
            return NocodeInitializationTableSupport.readText(cell.getStringCellValue(), field);
        } catch (RuntimeException exception) {
            throw invalid(cell, exception.getMessage());
        }
    }

    private static BigDecimal readNumber(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            throw invalid(cell, "日期请使用文本，时间戳请填写毫秒整数");
        }
        BigDecimal number = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
        if (number.abs().compareTo(new BigDecimal("1000000000000000")) >= 0 || number.precision() > 15) {
            throw invalid(cell, "超过 Excel 15 位有效数字，请先将单元格设为文本后重新输入原值");
        }
        return number;
    }

    private static IllegalArgumentException invalid(Cell cell, String message) {
        return invalid(cell.getSheet(), cell.getRowIndex(), cell.getColumnIndex(), message);
    }

    private static IllegalArgumentException invalid(Sheet sheet, int row, int column, String message) {
        return new IllegalArgumentException("初始化 Excel [" + sheet.getSheetName() + "!"
                + new CellReference(row, column).formatAsString() + "]: " + message);
    }
}
