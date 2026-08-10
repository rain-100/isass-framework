// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDate;

/**
 * @author Rain
 */
public class LocalDateToLongConvert extends StdConverter<LocalDate, Long> {

    @Override
    public Long convert(LocalDate value) {
        return LocalDateTimeUtil.localDateToEpochMilli(value);
    }

}
