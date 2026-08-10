// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 构建信息
 *
 * @author Rain
 * @since 1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JavaBuildInfo {

    private String group;

    private String artifact;

    private String name;

    private String version;

    private Long buildTime;

}
