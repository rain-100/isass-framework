// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalDateTimeConverter;

import java.time.LocalDateTime;

/**
 * @author Rain
 */
public class StringToLocalDateTimeConvert extends StdConverter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(String value) {
        return StringToLocalDateTimeConverter.convert0(value);
    }

}
