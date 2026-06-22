package vip.isass.framework.database.core.exception;

import lombok.Getter;
import vip.isass.framework.common.exception.IStatusMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.database.core.ModuleInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DatabaseStatusMapping implements IStatusMapping {

    private static final Map<Integer, IStatusMessage> STATUS_MAPPING = Arrays
            .stream(DatabaseStatusEnum.values())
            .collect(Collectors.toMap(DatabaseStatusEnum::getStatus, Function.identity()));

    @Override
    public IStatusMessage getErrorCode(Integer code) {
        return STATUS_MAPPING.get(code);
    }

    @Getter
    public enum DatabaseStatusEnum implements IStatusMessage {
        SQL_EXCEPTION(ModuleInfo.STATUS_CODE_PREFIX + 1001, "数据库错误"),
        TOO_MANY_RESULT(ModuleInfo.STATUS_CODE_PREFIX + 1002, "数据重复"),
        DUPLICATE_KEY(ModuleInfo.STATUS_CODE_PREFIX + 1003, "数据已存在"),
        BAD_SQL_GRAMMAR(ModuleInfo.STATUS_CODE_PREFIX + 1004, "sql错误"),
        DATASOURCE_CONNECT_FAIL(ModuleInfo.STATUS_CODE_PREFIX + 1005, "数据源连接失败"),
        ;

        private final Integer status;

        private final String msg;

        DatabaseStatusEnum(Integer status, String msg) {
            this.status = status;
            this.msg = msg;
        }
    }
}
