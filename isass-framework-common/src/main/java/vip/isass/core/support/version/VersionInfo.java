package vip.isass.core.support.version;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 版本信息
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
public class VersionInfo {

    private JavaBuildInfo javaBuildInfo;

    private GitInfo gitInfo;

}
