// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.selectoption;

import java.util.List;

/**
 * 选择选项服务接口
 *
 * @param <T> 选项值类型
 * @author Rain
 */
public interface ISelectOptionService<T> {

    String getKey();

    List<SelectOption<T>> getSelectOptions();
}
