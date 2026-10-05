// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.advanced;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Projects requested presentation fields without adding response behavior to entities. */
public final class AdvancedFeatureProjector {

    private static final Logger LOG = LoggerFactory.getLogger(AdvancedFeatureProjector.class);
    private static final String TEXT_SUFFIX = "Text";

    private final IDictTranslationProvider dictionaryProvider;

    public AdvancedFeatureProjector(IDictTranslationProvider dictionaryProvider) {
        this.dictionaryProvider = dictionaryProvider;
    }

    /** Only serialized properties are eligible, so excluded fields cannot reappear as derived values. */
    public Map<String, Object> project(Object entity, Set<String> serializedProperties, AdvancedFeature feature) {
        Map<String, Object> projected = new LinkedHashMap<>();
        if (entity == null || feature == null) {
            return projected;
        }
        if (feature.getDateFormat() != null) {
            feature.getDateFormat().forEach((property, pattern) -> {
                if (!eligible(serializedProperties, property) || pattern == null || pattern.isBlank()) return;
                Object value = ReflectUtil.getFieldValue(entity, property);
                if (value == null) return;
                String formatted = formatDate(value, pattern);
                if (formatted != null) projected.put(property + TEXT_SUFFIX, formatted);
            });
        }
        if (feature.getDecimalPlaces() != null) {
            feature.getDecimalPlaces().forEach((property, places) -> {
                if (!eligible(serializedProperties, property) || places == null) return;
                Object value = ReflectUtil.getFieldValue(entity, property);
                if (value == null) return;
                try {
                    projected.put(property + TEXT_SUFFIX, NumberUtil.round(value.toString(), places));
                } catch (Exception exception) {
                    LOG.warn("Cannot format decimal {}.{}", entity.getClass().getName(), property, exception);
                }
            });
        }
        if (dictionaryProvider != null && feature.getDictTranslation() != null) {
            feature.getDictTranslation().forEach((property, dictionary) -> {
                if (!eligible(serializedProperties, property) || dictionary == null || dictionary.isBlank()) return;
                Object value = ReflectUtil.getFieldValue(entity, property);
                if (value == null || value instanceof String text && text.isBlank()) return;
                try {
                    String translated = dictionaryProvider.translate(dictionary, value.toString());
                    if (translated != null) projected.put(property + TEXT_SUFFIX, translated);
                } catch (Exception exception) {
                    LOG.warn("Cannot translate dictionary {}.{}", entity.getClass().getName(), property, exception);
                }
            });
        }
        return projected;
    }

    private static boolean eligible(Set<String> serializedProperties, String property) {
        return property != null && serializedProperties != null && serializedProperties.contains(property);
    }

    private static String formatDate(Object value, String pattern) {
        if (value instanceof LocalDateTime dateTime) return DateUtil.format(dateTime, pattern);
        if (value instanceof LocalDate date) return DateUtil.format(LocalDateTimeUtil.toLocalDateTime(date), pattern);
        if (value instanceof LocalTime time) return DateUtil.format(LocalDateTimeUtil.toLocalDateTime(time), pattern);
        if (value instanceof Long timestamp) return DateUtil.format(LocalDateTimeUtil.toLocalDateTime(timestamp), pattern);
        if (value instanceof String text) return DateUtil.format(DateUtil.parse(text), pattern);
        return null;
    }
}
