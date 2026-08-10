// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * git 信息
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
public class GitInfo {

    private String branch;

    private String commitId;

    private String shortCommitId;

    private Long commitTime;

}
