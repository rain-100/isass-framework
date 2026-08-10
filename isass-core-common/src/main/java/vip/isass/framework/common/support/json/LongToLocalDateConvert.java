// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDate;

/**
 * @author Rain
 */
public class LongToLocalDateConvert extends StdConverter<Long, LocalDate> {

    @Override
    public LocalDate convert(Long value) {
        return LocalDateTimeUtil.epochMilliToLocalDate(value);
    }

}
