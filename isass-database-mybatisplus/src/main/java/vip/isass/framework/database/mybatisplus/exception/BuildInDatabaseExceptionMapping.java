// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.exception;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.BadSqlGrammarException;
import vip.isass.framework.common.exception.IExceptionMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;
import vip.isass.framework.database.core.exception.DatabaseStatusMapping;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

/**
 * @author Rain
 */
public class BuildInDatabaseExceptionMapping implements IExceptionMapping {

    private static Map<Class<? extends Exception>, IStatusMessage> EXCEPTION_MAPPING = MapUtil.<Class<? extends Exception>, IStatusMessage>builder()
        .put(DuplicateKeyException.class, DatabaseStatusMapping.DatabaseStatusEnum.DUPLICATE_KEY)
        .put(TooManyResultsException.class, DatabaseStatusMapping.DatabaseStatusEnum.TOO_MANY_RESULT)
        .put(BadSqlGrammarException.class, DatabaseStatusMapping.DatabaseStatusEnum.BAD_SQL_GRAMMAR)
        .build();

    @Override
    public IStatusMessage getStatusCode(Exception exception) {
        return EXCEPTION_MAPPING.get(exception.getClass());
    }

    @Override
    public String parseExceptionMessage(Throwable e) {
        Throwable unwrap = ExceptionUtil.unwrap(e);

        if (unwrap instanceof DuplicateKeyException) {
            DuplicateKeyException unwrap1 = (DuplicateKeyException) unwrap;
            SQLIntegrityConstraintViolationException cause = (SQLIntegrityConstraintViolationException) unwrap1.getCause();
            String message = cause.getMessage();
            String msg = ReUtil.get("Duplicate entry '(.*)' for key '.*'", message, 1);
            if (StrUtil.isNotBlank(msg)) {
                return msg;
            }
        }

        return unwrap.getMessage();
    }

}
