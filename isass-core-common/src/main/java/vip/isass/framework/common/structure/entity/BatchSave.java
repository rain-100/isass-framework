package vip.isass.framework.common.structure.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 批量保存请求
 *
 * @author rain
 * @since 1.0
 * @param <T> 实体类型
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("批量保存")
public class BatchSave<T> {

    // @ApiModelProperty(value = "需要新增的实体列表", dataType = "java.util.Map")
    @ApiModelProperty(value = "需要新增的实体列表")
    private List<T> addEntities;

    @ApiModelProperty(value = "需要修改的实体列表(根据id修改)")
    // @ApiModelProperty(value = "需要修改的实体列表(根据id修改)", dataType = "java.util.Map")
    private List<T> updateEntities;

    // @ApiModelProperty(value = "需要删除的实体id列表", dataType = "java.lang.String")
    @ApiModelProperty(value = "需要删除的实体id列表")
    private List<Serializable> deleteIds;

}
