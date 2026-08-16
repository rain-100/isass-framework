// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;

import java.util.List;
import java.util.Map;

/** Infrastructure endpoint for service-scoped NoCode initialization data. */
@RestController
@RequestMapping("/{serviceName:[a-zA-Z0-9-]+-service}/nocode/system/initialization")
public final class NocodeInitializationController {

    private final NocodeInitializationDataService dataService;

    public NocodeInitializationController(NocodeInitializationDataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/importData")
    public Resp<NocodeInitializationDataService.ImportResult> importData(
            @PathVariable String serviceName,
            @RequestBody Map<String, List<Map<String, Object>>> document) {
        return Resp.bizSuccess(dataService.importDataWithFailures(serviceName, document));
    }

    @GetMapping("/entities")
    public Resp<List<NocodeInitializationDataService.EntityInfo>> entities(@PathVariable String serviceName) {
        return Resp.bizSuccess(dataService.entities(serviceName));
    }

    @GetMapping("/exportData")
    public Resp<Map<String, List<?>>> exportData(@PathVariable String serviceName,
                                                 @RequestParam("entities") List<String> entities) {
        return Resp.bizSuccess(dataService.exportData(serviceName, entities));
    }
}
