package vip.isass.framework.nocode.v3.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
 * v3零代码接口
 * <p>
 * 通过 serviceName + entityName 路径变量动态路由到对应的 {@link IV3Service}，
 * 无需为每个实体编写独立的 Controller。
 * </p>
 *
 * @author Rain
 * @tag v3零代码接口
 */
@RestController
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

    /**
     * 新增实体
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON
     * @return 新增后的实体（含自增主键等回填字段）
     * @apiNote POST 请求，body 为实体 JSON。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_URI_SECOND_PART)
    public Object add(@PathVariable String serviceName,
                      @PathVariable String entityName,
                      @RequestBody Object body) {
        return requireService(serviceName, entityName).add((IV3Entity) body);
    }

    /**
     * 批量新增实体
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param entities    实体集合
     * @return 新增后的实体集合（含自增主键等回填字段）
     * @apiNote POST 请求，body 为实体 JSON 数组。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_URI_SECOND_PART)
    public Collection<?> addBatch(@PathVariable String serviceName,
                                   @PathVariable String entityName,
                                   @RequestBody Collection<?> entities) {
        return requireService(serviceName, entityName).addBatch((Collection) entities);
    }

    /**
     * 批量新增实体（指定批次大小）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param entities    实体集合
     * @param batchSize   每批次数量
     * @return 新增后的实体集合
     * @apiNote POST 请求，将实体按 batchSize 分批插入。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_BY_BATCH_SIZE_URI_SECOND_PART)
    public Collection<?> addBatchByBatchSize(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody Collection<?> entities,
                                              @PathVariable("batchSize") int batchSize) {
        return requireService(serviceName, entityName).addBatchByBatchSize((Collection) entities, batchSize);
    }

    /**
     * 按条件判断是否不存在则新增
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON
     * @param params      查询条件参数
     * @return 满足条件时新增后的实体，不满足时返回已有实体（或 null）
     * @apiNote POST 请求，先按 params 条件查询，不存在时才新增。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_IF_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Object addIfAbsentByCriteria(@PathVariable String serviceName,
                                         @PathVariable String entityName,
                                         @RequestBody Object body,
                                         @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addIfAbsentByCriteria((IV3Entity) body, criteria);
    }

    /**
     * 按唯一列判断是否不存在则新增
     *
     * @param serviceName   微服务名称
     * @param entityName    实体名称
     * @param body          实体 JSON
     * @param uniqueColumns 唯一列名集合
     * @return 新增后的实体，或已存在的实体
     * @apiNote POST 请求，根据 uniqueColumns 指定的唯一列判断，不存在则新增。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_IF_ABSENT_BY_COLUMNS_URI_SECOND_PART)
    public Object addIfAbsentByColumns(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestBody Object body,
                                        @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addIfAbsentByColumns((IV3Entity) body, uniqueColumns);
    }

    /**
     * 批量按条件判断是否不存在则新增
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param entities    实体集合
     * @param params      查询条件参数
     * @return 实际新增的数量
     * @apiNote POST 请求，按 params 条件逐条判断，不存在则批量插入。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_IF_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Integer addBatchIfAbsentByCriteria(@PathVariable String serviceName,
                                               @PathVariable String entityName,
                                               @RequestBody List<?> entities,
                                               @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addBatchIfAbsentByCriteria((List) entities, criteria);
    }

    /**
     * 批量按唯一列判断是否不存在则新增
     *
     * @param serviceName   微服务名称
     * @param entityName    实体名称
     * @param entities      实体集合
     * @param uniqueColumns 唯一列名集合
     * @return 实际新增的数量
     * @apiNote POST 请求，按 uniqueColumns 逐条判断是否存在，不存在则批量插入。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_BATCH_IF_ABSENT_BY_COLUMNS_URI_SECOND_PART)
    public Integer addBatchIfAbsentByColumns(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody List<?> entities,
                                              @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addBatchIfAbsentByColumns((List) entities, uniqueColumns);
    }

    /**
     * 按条件新增或更新
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON
     * @param params      查询条件参数
     * @return true 表示新增，false 表示更新
     * @apiNote POST 请求，按 params 条件查询，存在则更新、不存在则新增。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BY_CRITERIA_URI_SECOND_PART)
    public Boolean addOrUpdateByCriteria(@PathVariable String serviceName,
                                          @PathVariable String entityName,
                                          @RequestBody Object body,
                                          @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.addOrUpdateByCriteria((IV3Entity) body, criteria);
    }

    /**
     * 按唯一列新增或更新
     *
     * @param serviceName   微服务名称
     * @param entityName    实体名称
     * @param body          实体 JSON
     * @param uniqueColumns 唯一列名集合
     * @return 新增后的实体或更新后的实体
     * @apiNote POST 请求，按 uniqueColumns 判断，存在则更新、不存在则新增。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BY_COLUMNS_URI_SECOND_PART)
    public Object addOrUpdateByColumns(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestBody Object body,
                                        @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addOrUpdateByColumns((IV3Entity) body, uniqueColumns);
    }

    /**
     * 批量按唯一列新增或更新
     *
     * @param serviceName   微服务名称
     * @param entityName    实体名称
     * @param entities      实体集合
     * @param uniqueColumns 唯一列名集合
     * @return 实际处理的数量
     * @apiNote POST 请求，按 uniqueColumns 逐条判断，存在则更新、不存在则新增。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.ADD_OR_UPDATE_BATCH_BY_COLUMNS_URI_SECOND_PART)
    public Integer addOrUpdateBatchByColumns(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestBody List<?> entities,
                                              @PathVariable("uniqueColumns") List<String> uniqueColumns) {
        return requireService(serviceName, entityName).addOrUpdateBatchByColumns((List) entities, uniqueColumns);
    }

    // endregion

    //  region 删

    /**
     * 按主键删除
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param id          主键值
     * @return true 表示删除成功
     * @apiNote DELETE 请求，按主键 id 删除单条记录。
     */
    @DeleteMapping("/{serviceName}/{entityName}" + IV3Service.DELETE_BY_ID_URI_SECOND_PART)
    public Boolean deleteById(@PathVariable String serviceName,
                               @PathVariable String entityName,
                               @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).deleteById(id);
    }

    /**
     * 按主键集合批量删除
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param ids         主键值集合
     * @return true 表示删除成功
     * @apiNote DELETE 请求，按主键集合批量删除。
     */
    @DeleteMapping("/{serviceName}/{entityName}" + IV3Service.DELETE_BY_IDS_URI_SECOND_PART)
    public Boolean deleteByIds(@PathVariable String serviceName,
                                @PathVariable String entityName,
                                @PathVariable("ids") Collection<Serializable> ids) {
        return requireService(serviceName, entityName).deleteByIds(ids);
    }

    /**
     * 按条件删除
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return true 表示删除成功
     * @apiNote DELETE 请求，按 params 条件匹配并删除。
     */
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

    /**
     * 按主键更新
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON（须包含主键值）
     * @return true 表示更新成功
     * @apiNote PUT 请求，body 为实体 JSON，按主键定位并更新非空字段。
     */
    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_ID_URI_SECOND_PART)
    public Boolean updateById(@PathVariable String serviceName,
                               @PathVariable String entityName,
                               @RequestBody Object body) {
        return requireService(serviceName, entityName).updateById((IV3Entity) body);
    }

    /**
     * 按主键更新所有字段（含 null 覆盖）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON（须包含主键值）
     * @return true 表示更新成功
     * @apiNote PUT 请求，将实体的所有字段（含 null 值）更新到数据库对应行。
     */
    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_ALL_COLUMNS_BY_ID_URI_SECOND_PART)
    public Boolean updateAllColumnsById(@PathVariable String serviceName,
                                         @PathVariable String entityName,
                                         @RequestBody Object body) {
        return requireService(serviceName, entityName).updateAllColumnsById((IV3Entity) body);
    }

    /**
     * 按主键更新（不存在则抛异常）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON（须包含主键值）
     * @apiNote PUT 请求，按主键更新；记录不存在时抛出异常。
     */
    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_ID_OR_EXCEPTION_URI_SECOND_PART)
    public void updateByIdOrException(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestBody Object body) {
        requireService(serviceName, entityName).updateByIdOrException((IV3Entity) body);
    }

    /**
     * 按条件更新
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON
     * @param params      查询条件参数
     * @return true 表示更新成功
     * @apiNote PUT 请求，按 params 条件匹配并更新非空字段。
     */
    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_CRITERIA_URI_SECOND_PART)
    public Boolean updateByCriteria(@PathVariable String serviceName,
                                     @PathVariable String entityName,
                                     @RequestBody Object body,
                                     @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.updateByCriteria((IV3Entity) body, criteria);
    }

    /**
     * 按条件更新（不满足则抛异常）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param body        实体 JSON
     * @param params      查询条件参数
     * @apiNote PUT 请求，按 params 条件匹配并更新；无匹配记录时抛出异常。
     */
    @PutMapping("/{serviceName}/{entityName}" + IV3Service.UPDATE_BY_CRITERIA_OR_EXCEPTION_URI_SECOND_PART)
    public void updateByCriteriaOrException(@PathVariable String serviceName,
                                             @PathVariable String entityName,
                                             @RequestBody Object body,
                                             @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        service.updateByCriteriaOrException((IV3Entity) body, criteria);
    }

    /**
     * 批量保存（新增 + 更新 + 删除）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param batchSave   批量操作对象，内含待新增、更新、删除的实体集合
     * @apiNote POST 请求，一次性完成新增、更新和删除三合一操作。
     */
    @PostMapping("/{serviceName}/{entityName}" + IV3Service.BATCH_SAVE_URI_SECOND_PART)
    public void batchSave(@PathVariable String serviceName,
                           @PathVariable String entityName,
                           @RequestBody BatchSave<?> batchSave) {
        requireService(serviceName, entityName).batchSave(batchSave);
    }

    // endregion

    //  region 查

    /**
     * 按主键查询
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param id          主键值
     * @return 对应的实体，不存在则返回 null
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_ID_URI_SECOND_PART)
    public Object getById(@PathVariable String serviceName,
                           @PathVariable String entityName,
                           @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).getById(id);
    }

    /**
     * 按主键查询（不存在则抛异常）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param id          主键值
     * @return 对应的实体
     * @apiNote GET 请求，记录不存在时抛出异常。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_ID_OR_EXCEPTION_URI_SECOND_PART)
    public Object getByIdOrException(@PathVariable String serviceName,
                                      @PathVariable String entityName,
                                      @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).getByIdOrException(id);
    }

    /**
     * 按条件查询单条记录
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return 匹配的实体，有多条时返回第一条，无匹配返回 null
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_URI_SECOND_PART)
    public Object getByCriteria(@PathVariable String serviceName,
                                 @PathVariable String entityName,
                                 @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteria(criteria);
    }

    /**
     * 按条件查询单条记录（不存在则 warn）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return 匹配的实体，无匹配时记录 warn 日志并返回 null
     * @apiNote GET 请求，不抛异常仅在日志中输出 warn。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_OR_WARN_URI_SECOND_PART)
    public Object getByCriteriaOrWarn(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteriaOrWarn(criteria);
    }

    /**
     * 按条件查询单条记录（不存在则抛异常）
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return 匹配的实体
     * @apiNote GET 请求，无匹配记录时抛出异常。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.GET_BY_CRITERIA_OR_EXCEPTION_URI_SECOND_PART)
    public Object getByCriteriaOrException(@PathVariable String serviceName,
                                            @PathVariable String entityName,
                                            @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.getByCriteriaOrException(criteria);
    }

    /**
     * 按条件查询列表
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return 匹配的实体列表
     * @apiNote GET 请求，返回所有满足条件的记录。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_BY_CRITERIA_URI_SECOND_PART)
    public List<?> findByCriteria(@PathVariable String serviceName,
                                   @PathVariable String entityName,
                                   @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.findByCriteria(criteria);
    }

    /**
     * 按条件分页查询
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数（含分页参数 page、size）
     * @return 分页结果
     * @apiNote GET 请求，params 中传入 page、size 等分页参数。与默认列表查询共用部分 URI，借助 params 区分。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_PAGE_BY_CRITERIA_URI_SECOND_PART)
    public IPage<?> findPageByCriteria(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.findPageByCriteria(criteria);
    }

    /**
     * 查询所有记录
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @return 实体列表（含所有记录）
     * @apiNote GET 请求，返回全表数据，大数据量时慎用。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.FIND_ALL_URI_SECOND_PART)
    public List<?> findAll(@PathVariable String serviceName,
                            @PathVariable String entityName) {
        return requireService(serviceName, entityName).findAll();
    }

    /**
     * 按条件统计数量
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return 满足条件的记录数
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.COUNT_BY_CRITERIA_URI_SECOND_PART)
    public Integer countByCriteria(@PathVariable String serviceName,
                                    @PathVariable String entityName,
                                    @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.countByCriteria(criteria);
    }

    /**
     * 统计全表记录数
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @return 全表记录总数
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.COUNT_ALL_URI_SECOND_PART)
    public Integer countAll(@PathVariable String serviceName,
                             @PathVariable String entityName) {
        return requireService(serviceName, entityName).countAll();
    }

    /**
     * 按主键判断记录是否存在
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param id          主键值
     * @return true 表示存在，false 表示不存在
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_ID_URI_SECOND_PART)
    public Boolean isPresentById(@PathVariable String serviceName,
                                  @PathVariable String entityName,
                                  @PathVariable("id") Serializable id) {
        return requireService(serviceName, entityName).isPresentById(id);
    }

    /**
     * 按指定列值判断记录是否存在
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param columnName  列名
     * @param value       列值
     * @return true 表示存在，false 表示不存在
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_COLUMN_URI_SECOND_PART)
    public Boolean isPresentByColumn(@PathVariable String serviceName,
                                      @PathVariable String entityName,
                                      @PathVariable("columnName") String columnName,
                                      @PathVariable("value") Object value) {
        return requireService(serviceName, entityName).isPresentByColumn(columnName, value);
    }

    /**
     * 按条件判断记录是否存在
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return true 表示存在，false 表示不存在
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_PRESENT_BY_CRITERIA_URI_SECOND_PART)
    public Boolean isPresentByCriteria(@PathVariable String serviceName,
                                        @PathVariable String entityName,
                                        @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.isPresentByCriteria(criteria);
    }

    /**
     * 按指定列值判断记录是否不存在
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param columnName  列名
     * @param value       列值
     * @return true 表示不存在，false 表示存在
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_ABSENT_BY_COLUMN_URI_SECOND_PART)
    public Boolean isAbsentByColumn(@PathVariable String serviceName,
                                     @PathVariable String entityName,
                                     @PathVariable("columnName") String columnName,
                                     @PathVariable("value") Object value) {
        return requireService(serviceName, entityName).isAbsentByColumn(columnName, value);
    }

    /**
     * 按条件判断记录是否不存在
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @return true 表示不存在，false 表示存在
     * @apiNote GET 请求。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.IS_ABSENT_BY_CRITERIA_URI_SECOND_PART)
    public Boolean isAbsentByCriteria(@PathVariable String serviceName,
                                       @PathVariable String entityName,
                                       @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        return service.isAbsentByCriteria(criteria);
    }

    /**
     * 按条件检查，若记录存在则抛异常
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @apiNote GET 请求，满足条件时抛出异常，用于唯一性校验等场景。
     */
    @GetMapping("/{serviceName}/{entityName}" + IV3Service.EXCEPTION_IF_PRESENT_BY_CRITERIA_URI_SECOND_PART)
    public void exceptionIfPresentByCriteria(@PathVariable String serviceName,
                                              @PathVariable String entityName,
                                              @RequestParam Map<String, String> params) {
        IV3Service service = requireService(serviceName, entityName);
        IV3Criteria criteria = (IV3Criteria) criteriaMapper.toCriteria(params, service.criteriaClass());
        service.exceptionIfPresentByCriteria(criteria);
    }

    /**
     * 按条件检查，若记录不存在则抛异常
     *
     * @param serviceName 微服务名称
     * @param entityName  实体名称
     * @param params      查询条件参数
     * @apiNote GET 请求，不满足条件时抛出异常，用于存在性校验等场景。
     */
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
