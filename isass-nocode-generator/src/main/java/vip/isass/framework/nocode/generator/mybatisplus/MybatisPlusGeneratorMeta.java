// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import com.baomidou.mybatisplus.annotation.DbType;

/**
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class MybatisPlusGeneratorMeta {

    private DbType dbType;

    private String dataSourceUserName;

    private String dataSourcePassword;

    private String dataSourceUrl;

    private String schemaName;

    private String outputDir;

    /** DDD 限界上下文包名，例如 {@code attachment}。 */
    private String context;

    private String packageName;

    /** 服务根包（其中定义 ServiceInfo）；未设置时由 packageName 与 context 首段推导。 */
    private String serviceInfoPackageName;

    private String[] tablePrefix;

    private String[] includeTables;

    private String[] excludeTables;

    private String apiOutputDir;

    private String serviceOutputDir;

    private String controllerPrefix;

    private boolean entityFileOverride = true;

    private boolean criteriaFileOverride = true;

    private boolean mapperFileOverride = false;

    private boolean mapperXmlFileOverride = false;

    private boolean repositoryFileOverride = false;

    private boolean serviceInterfaceFileOverride = false;

    private boolean localServiceFileOverride = false;

    private boolean controllerFileOverride = false;

}
