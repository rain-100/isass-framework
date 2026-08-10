// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NetServerInfo {

    /**
     * 提供服务的协议
     */
    private NetProtocol netProtocol;

    /**
     * 外网 ip
     */
    private String externalIp;

    /**
     * 内网 ip
     */
    private String internalIp;

    /**
     * http 服务端口
     */
    private Integer httpPort;

    /**
     * http 服务端口是否使用 https
     */
    private Boolean httpSecure;

    /**
     * 网络服务外网端口
     */
    private Integer netExternalPort;

    /**
     * 网络服务外网 url
     */
    private String netExternalUrl;


}
