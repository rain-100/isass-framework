// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.selectoption;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * 选择选项类
 *
 * @param <T> 选项值类型
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class SelectOption<T> {
    private String name;
    private T value;
}
