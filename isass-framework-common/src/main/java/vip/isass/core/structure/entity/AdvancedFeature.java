package vip.isass.core.structure.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("高级特性")
public class AdvancedFeature {

    @ApiModelProperty("日期时间格式化")
    private Map<String, String> dateFormat;

    @ApiModelProperty("小数位数")
    private Map<String, Integer> decimalPlaces;

    @ApiModelProperty("字典翻译")
    private Map<String, String> dictTranslation;

}
