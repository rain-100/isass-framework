// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author rain
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MessageRedisKeyMapping {

    private String cmdPrefix;

    private String serviceName;

    private String redisKey;

}
