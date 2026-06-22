package vip.isass.framework.nocode.core.exception;

import lombok.Getter;
import vip.isass.framework.common.exception.IStatusMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.nocode.core.ModuleInfo;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NocodeStatusMapping implements IStatusMapping {

    private static final Map<Integer, IStatusMessage> STATUS_MAPPING = Arrays
            .stream(NocodeStatusEnum.values())
            .collect(Collectors.toMap(NocodeStatusEnum::getStatus, Function.identity()));

    @Override
    public IStatusMessage getErrorCode(Integer code) {
        return STATUS_MAPPING.get(code);
    }

    @Getter
    public enum NocodeStatusEnum implements IStatusMessage {
        UN_SUPPORT_OPERATION(ModuleInfo.STATUS_CODE_PREFIX + 1001, "暂不支持该操作:{}"),
        ASSOCIATED_DATA(ModuleInfo.STATUS_CODE_PREFIX + 1002, "存在关联数据"),
        ;

        private final Integer status;

        private final String msg;

        NocodeStatusEnum(Integer status, String msg) {
            this.status = status;
            this.msg = msg;
        }
    }
}
