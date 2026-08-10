// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.security.metadata.rolecode;

import vip.isass.framework.common.support.JsonUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * URI 角色编码请求
 *
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class UriRoleCodesReq {

    private String uri;

    @Override
    @SneakyThrows
    public String toString() {
        return JsonUtil.NOT_NULL_INSTANCE.writeValueAsString(this);
    }

}
