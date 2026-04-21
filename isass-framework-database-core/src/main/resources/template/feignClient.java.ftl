package ${cfg.feignPackage};

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.isass.core.web.Resp;
import vip.isass.core.web.feign.FeignEncoder;
import ${cfg.criteriaPackageName}.*;
import ${package.Entity}.*
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * <p>
 * ${table.comment!}
 * </p>
 *
 * @author ${author}
 */
@FeignClient(name = "isass-service-${cfg.moduleName}", url = "${r"$"}{feign.${cfg.moduleName}.url:}", fallback = ${entity}FeignFallBack.class)
public interface ${entity}FeignClient {

    // region ${cfg.prefix}_${table.name}

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/{id}")
    Resp<${entity}> get${entity}ById(@PathVariable("id") Serializable id);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/1")
    Resp<${entity}> get${entity}ByCriteria(@ModelAttribute ${entity}Criteria criteria);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/page")
    Resp<Page<${entity}>> find${entity}PageByCriteria(@ModelAttribute ${entity}Criteria criteria);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}")
    Resp<List<${entity}>> find${entity}ByCriteria(@ModelAttribute ${entity}Criteria criteria);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/count")
    Resp<Integer> count${entity}ByCriteria(@ModelAttribute ${entity}Criteria criteria);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/count/all")
    Resp<Integer> count${entity}All();

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/present/{id}")
    Resp<Boolean> is${entity}PresentById(@PathVariable("id") String id);

    @GetMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/present")
    Resp<Boolean> is${entity}PresentByCriteria(@ModelAttribute ${entity}Criteria criteria);

    @PostMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}")
    Resp<String> add${entity}(@RequestBody @Valid ${entity} entity);

    @PostMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/batch")
    Resp<Integer> batchAdd${entity}(@RequestBody ArrayList<${entity}> entitys);

    @PutMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/allColumns")
    Resp<Boolean> update${entity}AllColumnsById(@RequestBody @Valid ${entity} entity);

    @PutMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}")
    Resp<Boolean> update${entity}ExcludeNullFieldsById(@RequestBody @Valid ${entity} entity);

    @DeleteMapping("/v1/${table.name?replace("${cfg.tablePrefix[0]}_","","f")?replace("_","-")}/{ids}")
    Resp<Boolean> delete${entity}ByIds(@PathVariable("ids") List<String> ids);

    // endregion ${cfg.prefix}_${table.name}

}
