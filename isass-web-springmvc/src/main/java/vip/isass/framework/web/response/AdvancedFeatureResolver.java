// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.response;

import jakarta.servlet.http.HttpServletRequest;
import vip.isass.framework.nocode.entity.AdvancedFeature;

import java.util.LinkedHashMap;
import java.util.Map;

/** Resolves flat HTTP response-projection parameters into an explicit value object. */
final class AdvancedFeatureResolver {

    private static final String DATE_FORMAT = "dateFormat.";
    private static final String DECIMAL_PLACES = "decimalPlaces.";
    private static final String DICT_TRANSLATION = "dictTranslation.";

    private AdvancedFeatureResolver() {
    }

    static AdvancedFeature resolve(HttpServletRequest request) {
        Map<String, String> dateFormat = new LinkedHashMap<>();
        Map<String, Integer> decimalPlaces = new LinkedHashMap<>();
        Map<String, String> dictTranslation = new LinkedHashMap<>();
        request.getParameterMap().forEach((name, values) -> {
            String value = values.length == 0 ? null : values[0];
            if (value == null || value.isBlank()) {
                return;
            }
            if (name.startsWith(DATE_FORMAT)) {
                dateFormat.put(name.substring(DATE_FORMAT.length()), value);
            } else if (name.startsWith(DICT_TRANSLATION)) {
                dictTranslation.put(name.substring(DICT_TRANSLATION.length()), value);
            } else if (name.startsWith(DECIMAL_PLACES)) {
                try {
                    decimalPlaces.put(name.substring(DECIMAL_PLACES.length()), Integer.parseInt(value));
                } catch (NumberFormatException ignored) {
                    // Invalid projection options are ignored; they must not fail the business request.
                }
            }
        });
        return dateFormat.isEmpty() && decimalPlaces.isEmpty() && dictTranslation.isEmpty()
                ? null
                : AdvancedFeature.builder().dateFormat(dateFormat).decimalPlaces(decimalPlaces)
                .dictTranslation(dictTranslation).build();
    }
}
