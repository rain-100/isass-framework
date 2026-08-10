// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.isass.framework.common.support.JsonUtil;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "isass.json.object-mapper")
public class ObjectMapperConfiguration {

    /**
     * 不能设置为 false，会导致 bug
     * 例如 UserMobile 是idEntity ,但是没有id字段，在转为json时，{"userId":"1296279169555202049","mobile":"15949388631","id":null}
     * 会有 ”id“:null，导致翻序列化时，userId被id=null 覆盖，最终实体的userId没有值
     */
    private boolean usingNotNullObjectMapper = true;

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            JsonUtil.configure(builder);
            if (usingNotNullObjectMapper) {
                builder.changeDefaultPropertyInclusion(
                        v -> v.withValueInclusion(JsonInclude.Include.NON_NULL));
            }
        };
    }

}
