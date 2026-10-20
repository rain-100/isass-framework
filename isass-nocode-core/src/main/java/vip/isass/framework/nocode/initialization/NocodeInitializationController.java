// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Infrastructure endpoint for service-scoped NoCode initialization data. */
@RestController
@RequestMapping("/{serviceName:[a-zA-Z0-9-]+-service}/nocode/system/initialization")
public final class NocodeInitializationController {

    private final NocodeInitializationDataService dataService;
    private final NocodeInitializationImporter importer;

    public NocodeInitializationController(NocodeInitializationDataService dataService,
                                          NocodeInitializationImporter importer) {
        this.dataService = dataService;
        this.importer = importer;
    }

    @PostMapping("/importData")
    public Resp<ImportResult> importData(
            @PathVariable String serviceName,
            @RequestBody Map<String, List<Map<String, Object>>> document) {
        return Resp.bizSuccess(importer.importData(serviceName, document));
    }

    @PostMapping(value = "/importFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resp<ImportResult>> importFile(
            @PathVariable String serviceName,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, List<Map<String, Object>>> document;
        if (file == null || file.isEmpty()) {
            return invalidFile("初始化文件不能为空");
        }
        try (var input = file.getInputStream()) {
            document = importer.readDocument(input, file.getOriginalFilename());
        } catch (IOException | RuntimeException exception) {
            return invalidFile("无效初始化文件: " + exception.getMessage());
        }
        return ResponseEntity.ok(Resp.bizSuccess(importer.importData(serviceName, document)));
    }

    private ResponseEntity<Resp<ImportResult>> invalidFile(String message) {
        return ResponseEntity.badRequest().body(Resp.<ImportResult>bizFail().setMessage(message));
    }

    @GetMapping("/downloadTemplate")
    public ResponseEntity<?> downloadTemplate(
            @PathVariable String serviceName,
            @RequestParam(value = "entities", required = false) List<String> entities,
            @RequestParam(value = "format", defaultValue = "xlsx") String format) throws IOException {
        try {
            var types = importer.entityTypes(entities);
            if (format.equalsIgnoreCase("csv")) {
                var entity = csvEntity(types);
                return csvResponse("初始化模版", entity.getKey(),
                        NocodeInitializationCsv.write(entity.getKey(), entity.getValue(), List.of()));
            }
            if (!format.equalsIgnoreCase("xlsx")) {
                throw new IllegalArgumentException("模版格式仅支持 csv 或 xlsx");
            }
            byte[] template = NocodeInitializationExcelTemplate.create(types);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename("initialization-template.xlsx").build().toString())
                    .contentLength(template.length)
                    .body(template);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Resp.bizFail().setMessage(exception.getMessage()));
        }
    }

    @GetMapping("/entities")
    public Resp<List<NocodeInitializationDataService.EntityInfo>> entities(@PathVariable String serviceName) {
        return Resp.bizSuccess(importer.entities());
    }

    @GetMapping("/exportData")
    public ResponseEntity<?> exportData(@PathVariable String serviceName,
                                        @RequestParam("entities") List<String> entities,
                                        @RequestParam(value = "format", defaultValue = "json") String format) throws IOException {
        try {
            if (format.equalsIgnoreCase("csv")) {
                var entity = csvEntity(importer.entityTypes(entities));
                var rows = importer.exportData(serviceName, List.of(entity.getKey())).get(entity.getKey());
                return csvResponse("初始化数据", entity.getKey(),
                        NocodeInitializationCsv.write(entity.getKey(), entity.getValue(), rows));
            }
            if (!format.equalsIgnoreCase("json")) {
                throw new IllegalArgumentException("数据导出格式仅支持 csv 或 json");
            }
            return ResponseEntity.ok(Resp.bizSuccess(importer.exportData(serviceName, entities)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Resp.bizFail().setMessage(exception.getMessage()));
        }
    }

    private static Map.Entry<String, Class<?>> csvEntity(Map<String, Class<?>> types) {
        if (types.size() != 1) {
            throw new IllegalArgumentException("一个 CSV 文件只能对应一个实体，请每次传入一个实体资源名");
        }
        return types.entrySet().iterator().next();
    }

    private static ResponseEntity<byte[]> csvResponse(String business, String entity, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(business + "--" + entity + ".csv", StandardCharsets.UTF_8).build().toString())
                .contentLength(bytes.length).body(bytes);
    }
}
