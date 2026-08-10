// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.selectoption;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SelectOptionServiceManager {

    @Getter
    private Map<String, ISelectOptionService<?>> selectOptionServices = Collections.emptyMap();

    public SelectOptionServiceManager() {
    }

    public SelectOptionServiceManager(List<ISelectOptionService<?>> selectOptionServices) {
        setSelectOptionServices(selectOptionServices);
    }

    public void setSelectOptionServices(List<ISelectOptionService<?>> selectOptionServices) {
        this.selectOptionServices = selectOptionServices == null
                ? Collections.emptyMap()
                : selectOptionServices
                .stream()
                .collect(Collectors.toMap(ISelectOptionService::getKey, Function.identity()));
    }

}
