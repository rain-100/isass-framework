// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.exception;

import lombok.Getter;
import vip.isass.framework.common.exception.code.IStatusMessage;
import vip.isass.framework.web.security.ModuleInfo;

/**
 * @author Rain
 */
@Getter
public enum SecurityCoreStatusEnum implements IStatusMessage {

    UN_LOGIN(ModuleInfo.STATUS_CODE_PREFIX + 1001, "未登录系统"),
    JWT_TOKEN_ERROR(ModuleInfo.STATUS_CODE_PREFIX + 1002, "token错误或过期"),
    TOKEN_EXPIRED(ModuleInfo.STATUS_CODE_PREFIX + 1003, "token 已过期"),
    TOKEN_ILLEGAL(ModuleInfo.STATUS_CODE_PREFIX + 1004, "token 错误"),
    TOKEN_INVALID(ModuleInfo.STATUS_CODE_PREFIX + 1005, "token 无效"),
    VERIFICATION_CODE_ERROR(ModuleInfo.STATUS_CODE_PREFIX + 1006, "验证码错误"),
    VERIFICATION_CODE_ALREADY_SEND(ModuleInfo.STATUS_CODE_PREFIX + 1007, "请勿重复发送验证码"),
    FORCE_OFFLINE(ModuleInfo.STATUS_CODE_PREFIX + 1008, "强制下线"),
    FORBID_ONLINE_TERMINAL(ModuleInfo.STATUS_CODE_PREFIX + 1009, "终端已被禁用"),
    NOT_IN_ALLOW_TERMINAL_LIST(ModuleInfo.STATUS_CODE_PREFIX + 1010, "不在允许上线终端列表中"),
    OTHER_TERMINAL_ALREADY_LOGIN(ModuleInfo.STATUS_CODE_PREFIX + 1011, "登录失败，已在其他设备登陆"),
    APP_FORBID_ONLINE(ModuleInfo.STATUS_CODE_PREFIX + 1012, "应用已被禁用"),
    MAX_ONLINE_LIMIT(ModuleInfo.STATUS_CODE_PREFIX + 1013, "登录失败，已达到允许登录上限"),
    ;

    private final Integer status;

    private final String msg;

    SecurityCoreStatusEnum(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

}
