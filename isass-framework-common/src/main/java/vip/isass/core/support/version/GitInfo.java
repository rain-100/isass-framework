package vip.isass.core.support.version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * git 信息
 *
 * @author : rain
 * @date : 2022/12/14
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
