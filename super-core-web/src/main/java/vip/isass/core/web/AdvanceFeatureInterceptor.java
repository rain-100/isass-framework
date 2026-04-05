package vip.isass.core.web;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import vip.isass.core.structure.entity.AdvancedFeature;
import vip.isass.core.structure.entity.IAnyJsonEntity;
import vip.isass.core.support.JsonUtil;
import vip.isass.core.web.interceptor.IsassHandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 高级特性 拦截器
 *
 * @author : rain
 * @date : 2022/11/22
 */
@Slf4j
@Configuration
public class AdvanceFeatureInterceptor implements IsassHandlerInterceptor {

    private static final String ADVANCED_FEATURE_FIELD_NAME = "advancedFeature";

    private static final String ADVANCED_FEATURE_EQUAL = ADVANCED_FEATURE_FIELD_NAME + "=";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String query = request.getQueryString();
        if (StrUtil.isBlank(query) || !query.contains(ADVANCED_FEATURE_EQUAL)) {
            return true;
        }
        Map<String, String> paramMap = HttpUtil.decodeParamMap(query, StandardCharsets.UTF_8);
        String advancedFeatureStr = paramMap.get(ADVANCED_FEATURE_FIELD_NAME);
        if (StrUtil.isBlank(advancedFeatureStr)) {
            return true;
        }
        try {
            AdvancedFeature advancedFeature = JsonUtil.readValue(advancedFeatureStr, AdvancedFeature.class);
            IAnyJsonEntity.ADVANCED_FEATURE.set(advancedFeature);
        } catch (Exception e) {
            log.warn("request[{}] has error query string [{}], it must be json", request.getRequestURI(), ADVANCED_FEATURE_FIELD_NAME);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IAnyJsonEntity.ADVANCED_FEATURE.remove();
    }

}
