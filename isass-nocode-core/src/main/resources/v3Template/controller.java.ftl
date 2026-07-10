<#include "./segment/copyright.ftl">

package ${cfg.nocodeControllerPackageName};

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ${cfg.nocodeLocalServicePackageName}.V3${entity}Service;

/**
 * <p>
 * <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if> 手写扩展接口
 * </p>
 *
 * @apiNote 业务模块自定义接口。
 * @author ${author}
 * @tag <#if table.comment?trim?length gt 0>${table.comment}<#else>${entity}</#if>
 */
@Slf4j
@RestController
public class V3${entity}Controller {

    @Resource
    private V3${entity}Service v3${entity}Service;

}
