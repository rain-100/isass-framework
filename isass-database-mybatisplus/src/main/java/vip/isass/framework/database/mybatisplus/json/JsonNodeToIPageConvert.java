// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.json;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.JsonUtil;

public class JsonNodeToIPageConvert extends StdConverter<JsonNode, IPage<?>> {

    @Override
    public IPage<?> convert(JsonNode value) {
        return JsonUtil.DEFAULT_INSTANCE.convertValue(value, Page.class);
    }

}
