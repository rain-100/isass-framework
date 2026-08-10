// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.core.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;

import java.sql.SQLException;
import java.util.Map;

/**
 * @author Rain
 */
public class DatabaseExceptionMapping implements IExceptionMapping {

    private static final Map<Class<? extends Exception>, IStatusMessage> EXCEPTION_MAPPING = MapUtil.<Class<? extends Exception>, IStatusMessage>builder()
            .put(SQLException.class, DatabaseStatusMapping.DatabaseStatusEnum.SQL_EXCEPTION)
            .build();

    @Override
    public IStatusMessage getStatusCode(Exception exception) {
        Throwable unwrap = ExceptionUtil.unwrap(exception);
        return EXCEPTION_MAPPING.get(unwrap.getClass());
    }

    @Override
    public String parseExceptionMessage(Throwable e) {
        String message;
        Throwable unwrap = ExceptionUtil.unwrap(e);
        message = unwrap.getMessage();
        return message;
    }

}
