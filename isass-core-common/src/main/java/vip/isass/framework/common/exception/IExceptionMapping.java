// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception;

import cn.hutool.core.util.StrUtil;
import vip.isass.framework.common.exception.code.IStatusMessage;

/**
 * @author Rain
 */
public interface IExceptionMapping {

    IStatusMessage getStatusCode(Exception exception);

    /**
     * 从异常中格式化消息
     *
     * @param e 被抛出的异常
     * @return 格式化后的消息
     */
    String parseExceptionMessage(Throwable e);

    /**
     * 格式化消息，此信息将被终端显示
     *
     * @param t             被抛出的异常
     * @param statusMessage 状态消息
     * @return 格式化后的消息
     */
    default String parseMessage(Throwable t, IStatusMessage statusMessage) {
        if (StrUtil.isBlank(statusMessage.getMsg())) {
            return this.parseExceptionMessage(t);
        }

        String exceptionMessage = StrUtil.nullToEmpty(this.parseExceptionMessage(t));
        return statusMessage.getMsg().contains("{}")
            ? StrUtil.format(statusMessage.getMsg(), exceptionMessage)
            : statusMessage.getMsg() + StrUtil.prependIfMissing(exceptionMessage, ": ");
    }

}