package vip.isass.framework.common.structure.entity;
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
public class BatchSave<T> {

    private List<T> addEntities;
    private List<T> updateEntities;

    private List<Serializable> deleteIds;

}
