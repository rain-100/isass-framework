// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalTime;

/**
 * @author Rain
 */
public class LongToLocalTimeConvert extends StdConverter<Long, LocalTime> {

    @Override
    public LocalTime convert(Long value) {
        return LocalDateTimeUtil.epochMilliToLocalTime(value);
    }

}
