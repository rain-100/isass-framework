// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.isass.framework.common.support.LocalDateTimeUtil;
import vip.isass.framework.nocode.DictTranslationProviderUtil;
import vip.isass.framework.nocode.IDictTranslationProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Rain
 */
public interface IAnyJsonEntity {

    Logger log = LoggerFactory.getLogger(IAnyJsonEntity.class);

    String FORMATED_VALUE_SUFFIX = "Text";

    /**
     * 根据显式传入的响应投影选项生成扩展 JSON 字段。
     * Web、gRPC 与本地调用方均不依赖线程上下文。
     */
    default Map<String, Object> advancedJson(AdvancedFeature advancedFeature) {
        if (advancedFeature == null) {
            return null;
        }

        Map<String, Object> anyJsonMap = new HashMap<>();
        formatDataField(advancedFeature, anyJsonMap);
        formatDecimalPlaces(advancedFeature, anyJsonMap);
        dictTranslation(advancedFeature, anyJsonMap);
        return anyJsonMap;
    }

    default void dictTranslation(AdvancedFeature advanceFeature, Map<String, Object> anyJsonMap) {
        Map<String, String> dictTranslationMap = advanceFeature.getDictTranslation();
        if (MapUtil.isEmpty(dictTranslationMap)) {
            return;
        }
        IDictTranslationProvider dictTranslationProvider = DictTranslationProviderUtil.getProvider();
        if (dictTranslationProvider == null) {
            return;
        }
        for (Map.Entry<String, String> entry : dictTranslationMap.entrySet()) {
            if (StrUtil.isBlank(entry.getValue())) {
                continue;
            }
            Object fieldValue = ReflectUtil.getFieldValue(this, entry.getKey());
            if (fieldValue == null || StrUtil.isBlankIfStr(fieldValue)) {
                continue;
            }

            try {
                String text = dictTranslationProvider.translate(entry.getValue(), fieldValue.toString());
                if (text != null) {
                    anyJsonMap.put(entry.getKey() + FORMATED_VALUE_SUFFIX, text);
                }
            } catch (Exception e) {
                log.warn("entity[{}] field[{}] can not be scale decimal", this.getClass(), entry.getKey(), e);
            }
        }
    }

    default void formatDataField(AdvancedFeature advanceFeature, Map<String, Object> anyJsonMap) {
        Map<String, String> dateFormatMap = advanceFeature.getDateFormat();
        if (MapUtil.isEmpty(dateFormatMap)) {
            return;
        }

        for (Map.Entry<String, String> entry : dateFormatMap.entrySet()) {
            if (StrUtil.isBlank(entry.getValue())) {
                continue;
            }
            Object fieldValue = ReflectUtil.getFieldValue(this, entry.getKey());
            if (fieldValue == null) {
                continue;
            }

            String formatedValue = "";
            if (fieldValue instanceof LocalDateTime) {
                formatedValue = DateUtil.format((LocalDateTime) fieldValue, entry.getValue());
            } else if (fieldValue instanceof LocalDate) {
                formatedValue = DateUtil.format(
                        LocalDateTimeUtil.toLocalDateTime((LocalDate) fieldValue),
                        entry.getValue());
            } else if (fieldValue instanceof LocalTime) {
                formatedValue = DateUtil.format(
                        LocalDateTimeUtil.toLocalDateTime((LocalTime) fieldValue),
                        entry.getValue());
            } else if (fieldValue instanceof Long) {
                formatedValue = DateUtil.format(
                        LocalDateTimeUtil.toLocalDateTime((Long) fieldValue),
                        entry.getValue());
            } else if (fieldValue instanceof String) {
                formatedValue = DateUtil.format(DateUtil.parse((String) fieldValue), entry.getValue());
            }
            anyJsonMap.put(entry.getKey() + FORMATED_VALUE_SUFFIX, formatedValue);
        }
    }

    default void formatDecimalPlaces(AdvancedFeature advanceFeature, Map<String, Object> anyJsonMap) {
        Map<String, Integer> decimalPlacesMap = advanceFeature.getDecimalPlaces();
        if (MapUtil.isEmpty(decimalPlacesMap)) {
            return;
        }

        for (Map.Entry<String, Integer> entry : decimalPlacesMap.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            Object fieldValue = ReflectUtil.getFieldValue(this, entry.getKey());
            if (fieldValue == null) {
                continue;
            }
            try {
                anyJsonMap.put(
                        entry.getKey() + FORMATED_VALUE_SUFFIX,
                        NumberUtil.round(fieldValue.toString(), entry.getValue()));
            } catch (Exception e) {
                log.warn("entity[{}] field[{}] can not be scale decimal", this.getClass(), entry.getKey(), e);
            }
        }
    }
}
