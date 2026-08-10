// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.support.LocalDateTimeUtil;

import java.time.LocalDateTime;

/**
 * @author Rain
 */
public class LocalDateTimeToLongConvert extends StdConverter<LocalDateTime, Long> {

    @Override
    public Long convert(LocalDateTime value) {
        return LocalDateTimeUtil.localDateTimeToEpochMilli(value);
    }

}
