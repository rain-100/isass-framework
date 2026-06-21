package vip.isass.framework.nocode.v2.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * Batch save request for legacy v2 nocode services.
 *
 * @author rain
 * @since 1.0
 * @param <T> entity type
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
