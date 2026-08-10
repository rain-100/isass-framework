// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import vip.isass.framework.common.exception.code.IStatusMessage;

/**
 * 统一异常
 *
 * @author Rain
 */
@Getter
public class UnifiedException extends RuntimeException implements IStatusMessage {

    private Integer status;

    public UnifiedException(Throwable cause) {
        super(cause);
    }

    public UnifiedException(Throwable cause, Integer status) {
        super(cause);
        this.status = status;
    }

    public UnifiedException(IStatusMessage statusCode) {
        this(statusCode.getStatus(), statusCode.getMsg());
    }

    public UnifiedException(IStatusMessage statusCode, String msg) {
        this(statusCode.getStatus(), msg);
    }

    public UnifiedException(IStatusMessage statusCode, String msg, Object... args) {
        this(statusCode.getStatus(), msg, args);
    }

    public static UnifiedException newException(IStatusMessage statusCode, Object... args) {
        return new UnifiedException(statusCode.getStatus(), statusCode.getMsg(), args);
    }

    public UnifiedException(Integer status) {
        this.status = status;
    }

    public UnifiedException(Integer status, String msg) {
        super(msg);
        this.status = status;
    }

    public UnifiedException(Integer status, String msg, Object... args) {
        super(StrUtil.format(msg, args));
        this.status = status;
    }

    @Override
    public Integer getStatus() {
        return this.status;
    }

    @Override
    public String getMsg() {
        return this.getMessage();
    }

    @Override
    public String toString() {
        return status + ": " + getMessage();
    }
}
