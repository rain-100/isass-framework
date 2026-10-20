// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import vip.isass.framework.common.support.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** One UTF-8 CSV file is one resource; both template and data exports contain two header records. */
final class NocodeInitializationCsv {

    private NocodeInitializationCsv() {
    }

    static Map<String, List<Map<String, Object>>> read(InputStream input, String filename,
                                                     Function<String, Class<?>> entityTypes) throws IOException {
        String entity = entityName(filename);
        Map<String, Field> metadata = NocodeInitializationTableSupport.fields(entityTypes.apply(entity));
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new PushbackReader(new InputStreamReader(input, decoder), 1)) {
            int first = reader.read();
            if (first != -1 && first != '\uFEFF') {
                reader.unread(first);
            }
            try (var parser = CSVFormat.RFC4180.parse(reader)) {
                var records = parser.getRecords();
                if (records.isEmpty()) {
                    throw invalid(filename, 1, 1, "第一条记录必须填写字段名");
                }
                List<String> fields = new ArrayList<>();
                Set<String> unique = new LinkedHashSet<>();
                for (String name : records.getFirst()) {
                    String field = name.trim();
                    if (!field.matches("[a-zA-Z_$][a-zA-Z0-9_$]*") || !unique.add(field)) {
                        throw invalid(filename, 1, fields.size() + 1, "字段名无效或重复: " + field);
                    }
                    fields.add(field);
                }
                List<Map<String, Object>> rows = new ArrayList<>();
                for (int index = 2; index < records.size(); index++) {
                    var record = records.get(index);
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (int column = 0; column < record.size(); column++) {
                        String text = record.get(column);
                        if (text.isEmpty()) {
                            continue;
                        }
                        if (column >= fields.size()) {
                            throw invalid(filename, index + 1, column + 1, "数据列缺少对应字段名");
                        }
                        String field = fields.get(column);
                        try {
                            values.put(field, NocodeInitializationTableSupport.readText(text, metadata.get(field)));
                        } catch (RuntimeException exception) {
                            throw invalid(filename, index + 1, column + 1, exception.getMessage());
                        }
                    }
                    if (!values.isEmpty()) {
                        rows.add(values);
                    }
                }
                return Map.of(entity, rows);
            }
        } catch (IOException | UncheckedIOException exception) {
            throw new IOException("初始化 CSV [" + filename + "]: " + exception.getMessage(), exception);
        }
    }

    static String entityName(String filename) {
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("初始化 CSV 文件名必须以 .csv 结尾");
        }
        name = name.substring(0, name.length() - 4);
        int separator = name.lastIndexOf("--");
        String entity = separator < 0 ? name : name.substring(separator + 2);
        if (separator == 0 || !entity.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            throw new IllegalArgumentException("CSV 文件名应为 业务标识--实体资源名.csv 或 实体资源名.csv: " + filename);
        }
        return entity;
    }

    static byte[] write(String entity, Class<?> entityType, Collection<?> rows) throws IOException {
        if (!entity.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            throw new IllegalArgumentException("无效实体资源名: " + entity);
        }
        List<Field> fields = NocodeInitializationTableSupport.templateFields(entityType);
        var output = new StringWriter();
        output.write('\uFEFF');
        try (var printer = new CSVPrinter(output, CSVFormat.RFC4180)) {
            printer.printRecord(fields.stream().map(Field::getName).toList());
            printer.printRecord(fields.stream().map(NocodeInitializationTableSupport::description).toList());
            for (Object row : rows) {
                Object entityRow = entityType.isInstance(row) ? row : JsonUtil.convertValue(row, entityType);
                List<String> values = new ArrayList<>();
                for (Field field : fields) {
                    try {
                        if (!field.trySetAccessible()) {
                            throw new IllegalAccessException(field.getName());
                        }
                        values.add(NocodeInitializationTableSupport.writeValue(field.get(entityRow), field));
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("无法导出字段: " + entity + "." + field.getName(), exception);
                    }
                }
                printer.printRecord(values);
            }
        }
        return output.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static IllegalArgumentException invalid(String filename, int record, int column, String message) {
        return new IllegalArgumentException("初始化 CSV [" + filename + " 第" + record + "条记录，第" + column + "列]: " + message);
    }
}
