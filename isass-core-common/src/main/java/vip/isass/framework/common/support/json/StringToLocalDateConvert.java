// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.json;

import tools.jackson.databind.util.StdConverter;
import vip.isass.framework.common.converter.datatime.StringToLocalDateConverter;

import java.time.LocalDate;

/**
 * @author Rain
 */
public class StringToLocalDateConvert extends StdConverter<String, LocalDate> {

    @Override
    public LocalDate convert(String value) {
        return StringToLocalDateConverter.convert0(value);
    }

}
