// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode;

import cn.hutool.core.collection.CollUtil;
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;
import vip.isass.framework.nocode.entity.ITraceEntity;

import java.util.HashSet;

/**
 * 敏感数据属性名
 * 查数据库时，默认不查询这些字段
 *
 * @author Rain
 */
public interface SensitiveDataProperty {

    HashSet<String> PROPERTIES = CollUtil.newHashSet(
            "deleteFlag",
            //        TimeTracedEntity.CREATED_TIME_PROPERTY,
            //        TimeTracedEntity.MODIFY_TIME_PROPERTY,
            "createUserId",
            "createUserName",
            "modifyUserId",
            "modifyUserName",

            "password");

}
