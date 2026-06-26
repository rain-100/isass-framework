package vip.isass.framework.nocode.v3.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import vip.isass.framework.common.support.api.ApiOrder;
import vip.isass.framework.nocode.v3.V3CriteriaMapper;
import vip.isass.framework.nocode.v3.V3ServiceRegistry;
import vip.isass.framework.nocode.v3.entity.BatchSave;
import vip.isass.framework.nocode.v3.entity.IV3Entity;
import vip.isass.framework.nocode.v3.criteria.IV3Criteria;
import vip.isass.framework.nocode.v3.service.IV3Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * v3 通用 CRUD Controller。
 * <p>
 * 通过 serviceName + entityName 路径变量动态路由到对应的 {@link IV3Service}，
 * 无需为每个实体编写独立的 Controller。
 * </p>
 *
 * @author Rain
 * @tag 通用CRUD
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class V3Controller {

    private final V3ServiceRegistry serviceRegistry;
    private final V3CriteriaMapper criteriaMapper;

    public V3Controller(V3ServiceRegistry serviceRegistry, V3CriteriaMapper criteriaMapper) {
        this.serviceRegistry = serviceRegistry;
        this.criteriaMapper = criteriaMapper;
    }

    public int getOrder() {
        return ApiOrder.CONTROLLER;
    }

    private IV3Service requireService(String serviceName, String entityName) {
        return serviceRegistry.require(serviceName, entityName);
    }

    // region 增

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_URI_SECOND_PART)
    public Object add(@PathVariable String serviceName,
                      @PathVariable String entityName,
                      @RequestBody Object body) {
        return requireService(serviceName, entityName).add((IV3Entity) body);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_URI_SECOND_PART)
    public Collection<?> addBatch(@PathVariable String serviceName,
                                   @PathVariable String entityName,
                                   @RequestBody Collection<?> entities) {
        return requireService(serviceName, entityName).addBatch((Collection) entities);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_BY_BATCH_SIZE_URI_SECOND_PART)
    public Collection<?> addBatchByBatchSize(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody Collection<?> entities,
                                              @PathVariable("batchSize") int batchSize) {
        return requireService(serviceName, entityName).addBatchByBatchSize((Collection) entities, batchSize);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_IF_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Object addIfAbsentByCriteria(@PathVariable String serviceName,
                                         @PathVariable String entityName,
                                         @RequestBody Object body,
                                         @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addIfAbsentByCriteria((IV3Entity) body, criteria);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_IF_ABSENT_BY_COLUMNS_URI_SECOND_PART)
    public Object addIfAbsentByColumns(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestBody Object body,
                                        @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addIfAbsentByColumns((IV3Entity) body, uniqueColumns);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_IF_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Integer addBatchIfAbsentByCriteria(@PathVariable String serviceName,
                                               @PathVariable String entityName,
                                               @RequestBody List<?> entities,
                                               @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addBatchIfAbsentByCriteria((List) entities, criteria);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_IF_ABSENT_BY_COLUMNS_URI_SECOND_PART)
    public Integer addBatchIfAbsentByColumns(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody List<?> entities,
                                              @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addBatchIfAbsentByColumns((List) entities, uniqueColumns);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BY_CRITERIA_URI_SECOND_PART)
    public Boolean addOrUpdateByCriteria(@PathVariable String serviceName,
                                          @PathVariable String entityName,
                                          @RequestBody Object body,
                                          @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addOrUpdateByCriteria((IV3Entity) body, criteria);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BY_COLUMNS_URI_SECOND_PART)
    public Object addOrUpdateByColumns(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestBody Object body,
                                        @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addOrUpdateByColumns((IV3Entity) body, uniqueColumns);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BATCH_BY_COLUMNS_URI_SECOND_PART)
    public Integer addOrUpdateBatchByColumns(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody List<?> entities,
                                              @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addOrUpdateBatchByColumns((List) entities, uniqueColumns);
    }

    // endregion

    //  region 删

    @DeleteMapping("/{serviceName}/{entityName}" + IV3Service.DELETE_BY_ID_URI_SECOND_PART)
    public Boolean deleteById(@PathVariable String serviceName,
                               @PathVariable String entityName,
                               @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).deleteById(id);
    }

    @DeleteMapping("/{serviceName}/{entityName}" + IV3Service.DELETE_BY_IDS_URI_SECOND_PART)
    public Boolean deleteByIds(@PathVariable String serviceName,
                                @PathVariable String entityName,
                                @PathVariable("ids") Collection<Serializable> ids) {
        return requireService(serviceName, entityName).deleteByIds(ids);
    }

    @DeleteMapping("/{serviceName}/{entityName}" + IV3Service.DELETE_BY_CRITERIA_URI_SECOND_PART)
    public Boolean deleteByCriteria(@PathVariable String serviceName,
                                     @PathVariable String entityName,
                                     @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.deleteByCriteria(criteria);
    }

    // endregion

    // region 改

    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_ID_URI_SECOND_PART)
    public Boolean updateById(@PathVariable String serviceName,
                               @PathVariable String entityName,
                               @RequestBody Object body) {
        return requireService(serviceName, entityName).updateById((IV3Entity) body);
    }

    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_ALL_COLUMNS_BY_ID_URI_SECOND_PART)
    public Boolean updateAllColumnsById(@PathVariable String serviceName,
                                         @PathVariable String entityName,
                                         @RequestBody Object body) {
        return requireService(serviceName, entityName).updateAllColumnsById((IV3Entity) body);
    }

    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_ID_OR_EXCEPTION_URI_SECOND_PART)
    public void updateByIdOrException(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestBody Object body) {
        requireService(serviceName, entityName).updateByIdOrException((IV3Entity) body);
    }

    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_CRITERIA_URI_SECOND_PART)
    public Boolean updateByCriteria(@PathVariable String serviceName,
                                     @PathVariable String entityName,
                                     @RequestBody Object body,
                                     @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.updateByCriteria((IV3Entity) body, criteria);
    }

    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_CRITERIA_OR_EXCEPTION_URI_SECOND_PART)
    public void updateByCriteriaOrException(@PathVariable String serviceName,
                                             @PathVariable String entityName,
                                             @RequestBody Object body,
                                             @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        service.updateByCriteriaOrException((IV3Entity) body, criteria);
    }

    @PostMapping("/{serviceName}/{entityName}" + IV3Service.BATCH_SAVE_URI_SECOND_PART)
    public void batchSave(@PathVariable String serviceName,
                           @PathVariable String entityName,
                           @RequestBody BatchSave<?> batchSave) {
        requireService(serviceName, entityName).batchSave(batchSave);
    }

    // endregion

    //  region 查

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_ID_URI_SECOND_PART)
    public Object getById(@PathVariable String serviceName,
                           @PathVariable String entityName,
                           @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).getById(id);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_ID_OR_EXCEPTION_URI_SECOND_PART)
    public Object getByIdOrException(@PathVariable String serviceName,
                                      @PathVariable String entityName,
                                      @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).getByIdOrException(id);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_URI_SECOND_PART)
    public Object getByCriteria(@PathVariable String serviceName,
                                 @PathVariable String entityName,
                                 @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_OR_WARN_URI_SECOND_PART)
    public Object getByCriteriaOrWarn(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteriaOrWarn(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_OR_EXCEPTION_URI_SECOND_PART)
    public Object getByCriteriaOrException(@PathVariable String serviceName,
                                            @PathVariable String entityName,
                                            @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteriaOrException(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_BY_CRITERIA_URI_SECOND_PART)
    public List<?> findByCriteria(@PathVariable String serviceName,
                                   @PathVariable String entityName,
                                   @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.findByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_PAGE_BY_CRITERIA_URI_SECOND_PART)
    public IPage<?> findPageByCriteria(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.findPageByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_ALL_URI_SECOND_PART)
    public List<?> findAll(@PathVariable String serviceName,
                            @PathVariable String entityName) {
        return requireService(serviceName, entityName).findAll();
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.COUNT_BY_CRITERIA_URI_SECOND_PART)
    public Integer countByCriteria(@PathVariable String serviceName,
                                    @PathVariable String entityName,
                                    @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.countByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.COUNT_ALL_URI_SECOND_PART)
    public Integer countAll(@PathVariable String serviceName,
                             @PathVariable String entityName) {
        return requireService(serviceName, entityName).countAll();
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_ID_URI_SECOND_PART)
    public Boolean isPresentById(@PathVariable String serviceName,
                                  @PathVariable String entityName,
                                  @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).isPresentById(id);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_COLUMN_URI_SECOND_PART)
    public Boolean isPresentByColumn(@PathVariable String serviceName,
                                      @PathVariable String entityName,
                                      @PathVariable("columnName") String columnName,
                                      @PathVariable("value") Object value) {
        return requireService(serviceName, entityName).isPresentByColumn(columnName, value);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_CRITERIA_URI_SECOND_PART)
    public Boolean isPresentByCriteria(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.isPresentByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_ABSENT_BY_COLUMN_URI_SECOND_PART)
    public Boolean isAbsentByColumn(@PathVariable String serviceName,
                                     @PathVariable String entityName,
                                     @PathVariable("columnName") String columnName,
                                     @PathVariable("value") Object value) {
        return requireService(serviceName, entityName).isAbsentByColumn(columnName, value);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Boolean isAbsentByCriteria(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.isAbsentByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.EXCEPTION_IF_PRESENT_BY_CRITERIA_URI_SECOND_PART)
    public void exceptionIfPresentByCriteria(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        service.exceptionIfPresentByCriteria(criteria);
    }

    @GetMapping("/{serviceName}/{entityName}" + IV3Service.EXCEPTION_IF_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public void exceptionIfAbsentByCriteria(@PathVariable String serviceName,
                                             @PathVariable String entityName,
                                             @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        service.exceptionIfAbsentByCriteria(criteria);
    }

    // endregion

}
