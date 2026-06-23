package vip.isass.framework.web.nocode;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.MultiValueMap;
import vip.isass.framework.nocode.v3.operation.NocodeCrudOperation;
import vip.isass.framework.nocode.v3.query.NocodeQueryCriteria;

/**
 * 通用 CRUD Controller，按 {@link NocodeSpringMvcCrudRoute#defaultRoutes()} 路径暴露 nocode v3 操作。
 * <p>
 * 路由模式：
 * <ul>
 *   <li>GET    /nocode/{entityName}          → list</li>
 *   <li>GET    /nocode/{entityName}/page     → page</li>
 *   <li>GET    /nocode/{entityName}/{id}     → findById</li>
 *   <li>POST   /nocode/{entityName}          → save</li>
 *   <li>PUT    /nocode/{entityName}/{id}     → updateById</li>
 *   <li>DELETE /nocode/{entityName}/{id}     → deleteById</li>
 * </ul>
 *
 * @author Rain
 */
@RestController
@RequestMapping("/nocode")
public class NocodeCrudController {

    private final NocodeSpringMvcCrudEndpointInvoker invoker;

    private final NocodeSpringMvcQueryCriteriaParser queryCriteriaParser;

    public NocodeCrudController(NocodeSpringMvcCrudEndpointInvoker invoker,
                                 NocodeSpringMvcQueryCriteriaParser queryCriteriaParser) {
        this.invoker = invoker;
        this.queryCriteriaParser = queryCriteriaParser;
    }

    @GetMapping("/{entityName}")
    public Object list(
            @PathVariable("entityName") String entityName,
            @RequestParam MultiValueMap<String, String> queryParams) {
        NocodeQueryCriteria criteria = queryCriteriaParser.parse(queryParams);
        return invoker.invoke(
                route(NocodeCrudOperation.LIST),
                NocodeSpringMvcCrudRequestArguments.query(entityName, criteria));
    }

    @GetMapping("/{entityName}/page")
    public Object page(
            @PathVariable("entityName") String entityName,
            @RequestParam MultiValueMap<String, String> queryParams) {
        NocodeQueryCriteria criteria = queryCriteriaParser.parse(queryParams);
        return invoker.invoke(
                route(NocodeCrudOperation.PAGE),
                NocodeSpringMvcCrudRequestArguments.query(entityName, criteria));
    }

    @GetMapping("/{entityName}/{id}")
    public Object findById(
            @PathVariable("entityName") String entityName,
            @PathVariable("id") String id) {
        return invoker.invoke(
                route(NocodeCrudOperation.FIND_BY_ID),
                NocodeSpringMvcCrudRequestArguments.byId(entityName, id));
    }

    @PostMapping("/{entityName}")
    public Object save(
            @PathVariable("entityName") String entityName,
            @RequestBody(required = false) Object body) {
        return invoker.invoke(
                route(NocodeCrudOperation.SAVE),
                NocodeSpringMvcCrudRequestArguments.body(entityName, body));
    }

    @PutMapping("/{entityName}/{id}")
    public Object updateById(
            @PathVariable("entityName") String entityName,
            @PathVariable("id") String id,
            @RequestBody(required = false) Object body) {
        return invoker.invoke(
                route(NocodeCrudOperation.UPDATE_BY_ID),
                NocodeSpringMvcCrudRequestArguments.bodyById(entityName, id, body));
    }

    @DeleteMapping("/{entityName}/{id}")
    public Object deleteById(
            @PathVariable("entityName") String entityName,
            @PathVariable("id") String id) {
        return invoker.invoke(
                route(NocodeCrudOperation.DELETE_BY_ID),
                NocodeSpringMvcCrudRequestArguments.byId(entityName, id));
    }

    private static NocodeSpringMvcCrudRoute route(NocodeCrudOperation operation) {
        return NocodeSpringMvcCrudRoute.defaultRoutes().stream()
                .filter(r -> r.operation() == operation)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No route for " + operation));
    }
}
