// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalTimeConverter;

import java.time.LocalTime;

/**
 * @author Rain
 */
public class StringToLocalTimeConvert extends StdConverter<String, LocalTime> {

    @Override
    public LocalTime convert(String value) {
        return StringToLocalTimeConverter.convert0(value);
    }

}
