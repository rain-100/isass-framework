// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.map.MapUtil;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.DateTimeException;
import java.util.Map;

/**
 * @author Rain
 */
public class BuildInCoreExceptionMapping implements IExceptionMapping {

    private static final Map<Class<? extends Exception>, IStatusMessage> EXCEPTION_MAPPING = MapUtil.<Class<? extends Exception>, IStatusMessage>builder()
            .put(IllegalArgumentException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
            .put(AlreadyPresentException.class, StatusMessageEnum.ALREADY_PRESENT)
            .put(AbsentException.class, StatusMessageEnum.ABSENT)
            .put(UnsupportedOperationException.class, StatusMessageEnum.UN_SUPPORT_OPERATION)
            .put(ValidateException.class, StatusMessageEnum.ILLEGAL_ARGUMENT_ERROR)
            .put(IOException.class, StatusMessageEnum.IO_ERROR)
            .put(FileNotFoundException.class, StatusMessageEnum.FILE_NOT_FOUND)
            .put(DateTimeException.class, StatusMessageEnum.DATE_TIME_ERROR)
            .build();

    @Override
    public IStatusMessage getStatusCode(Exception exception) {
        Throwable unwrap = ExceptionUtil.unwrap(exception);
        return EXCEPTION_MAPPING.get(unwrap.getClass());
    }

    public String parseExceptionMessage(Throwable e) {
        Throwable unwrap = ExceptionUtil.unwrap(e);
        return unwrap.getMessage();
    }

}
