// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.mybatisplus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusGeneratorTest {

    @Test
    void includeTablesRestrictsDatabaseDiscovery() {
        String[] includes = {"bsp_log_request_log"};

        assertThat(MybatisPlusGenerator.isTableSelected("bsp_log_request_log", includes, null)).isTrue();
        assertThat(MybatisPlusGenerator.isTableSelected("bsp_auth_user", includes, null)).isFalse();
    }

    @Test
    void excludeTablesSupportsRegularExpressions() {
        String[] excludes = {"(?i)(.*_)?DATABASECHANGELOG", "(?i)(.*_)?DATABASECHANGELOGLOCK"};

        assertThat(MybatisPlusGenerator.isTableSelected("DATABASECHANGELOG", null, excludes)).isFalse();
        assertThat(MybatisPlusGenerator.isTableSelected("bsp_log_request_log", null, excludes)).isTrue();
    }
}
