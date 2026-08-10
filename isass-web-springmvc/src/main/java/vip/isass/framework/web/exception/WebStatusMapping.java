// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.exception;

import cn.hutool.core.map.MapUtil;
import lombok.Getter;
import vip.isass.framework.common.exception.IStatusMapping;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.common.exception.code.StatusMessageEnum;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WebStatusMapping implements IStatusMapping {

    private static final Map<Integer, IStatusMessage> statusMapping = MapUtil
        .<Integer, IStatusMessage>builder()
        .put(403, StatusMessageEnum.ACCESS_DENIED_403)
        .put(404, StatusMessageEnum.NOT_FOUND_404)
        .put(405, StatusMessageEnum.METHOD_NOT_ALLOWED_405)
        .put(500, StatusMessageEnum.INTERNAL_SERVER_ERROR_500)
        .putAll(Arrays
            .stream(WebStatusEnum.values())
            .collect(Collectors.toMap(WebStatusEnum::getStatus, Function.identity())))
        .build();

    @Override
    public IStatusMessage getErrorCode(Integer code) {
        Map<Integer, WebStatusEnum> collect = Arrays.stream(WebStatusEnum.values())
            .collect(Collectors.toMap(WebStatusEnum::getStatus, Function.identity()));
        return statusMapping.get(code);
    }

    @Getter
    public enum WebStatusEnum implements IStatusMessage {

        FEIGN_URL_NOT_FOUND(10027_00001, "feign请求[{}]404"),
        ;

        private final Integer status;

        private final String msg;

        WebStatusEnum(Integer status, String msg) {
            this.status = status;
            this.msg = msg;
        }

        @Override
        public String getMsg() {
            return this.msg;
        }

        @Override
        public Integer getStatus() {
            return this.status;
        }
    }

}
