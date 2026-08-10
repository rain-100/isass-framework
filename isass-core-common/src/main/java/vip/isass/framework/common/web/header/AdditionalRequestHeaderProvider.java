// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.header;

/**
 * 附加请求头提供者接口
 *
 * @author Rain
 */
public interface AdditionalRequestHeaderProvider {

    String getHeaderName();

    String getValue();

    /**
     * 当已存在同名的请求头时，是否覆盖旧的值
     *
     * @return is override
     */
    boolean override();

    boolean support(String method, String uri);

}
