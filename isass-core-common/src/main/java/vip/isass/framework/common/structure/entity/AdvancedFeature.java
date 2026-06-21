package vip.isass.framework.common.structure.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * 高级特性配置
 *
 * @author Rain
 * @since 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdvancedFeature {
    private Map<String, String> dateFormat;
    private Map<String, Integer> decimalPlaces;
    private Map<String, String> dictTranslation;

}
