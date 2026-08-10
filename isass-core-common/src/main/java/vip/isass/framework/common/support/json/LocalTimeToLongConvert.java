// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalTime;

/**
 * @author Rain
 */
public class LocalTimeToLongConvert extends StdConverter<LocalTime, Long> {

    @Override
    public Long convert(LocalTime value) {
        return LocalDateTimeUtil.localTimeToEpochMilli(value);
    }

}
