// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * @author : rain
 * @date : 2022/11/24
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedFeature {

    /**
     * 日期时间格式化
     */
    private Map<String, String> dateFormat;

    /**
     * 小数位数
     */
    private Map<String, Integer> decimalPlaces;

    /**
     * 字典翻译
     */
    private Map<String, String> dictTranslation;

}
