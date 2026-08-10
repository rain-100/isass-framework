// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.exception.code;

import lombok.Getter;

/**
 * @author Rain
 */
@Getter
public enum StatusMessageEnum implements IStatusMessage {

    UNDEFINED(-2, "未定义错误"),
    FAIL(-1, "操作失败"),
    SUCCESS(200, "操作成功"),

    ALREADY_PRESENT(12, "数据已存在"),
    ABSENT(13, "数据不存在"),
    UN_SUPPORT_OPERATION(14, "暂不支持该操作:{}"),

    // 安全相关（core-common JwtUtil/CurrentPrincipalUtil/IsassErrorController 引用，不迁至 security）
    JWT_TOKEN_ERROR(15, "token错误或过期"),
    UN_LOGIN(16, "未登录系统"),

    FEIGN_ERROR(18, "feign请求异常"),
    IO_ERROR(22, "io异常"),
    FILE_NOT_FOUND(23, "文件不存在"),

    ACCESS_DENIED_403(403, "权限不足"),
    NOT_FOUND_404(404, "资源不存在"),
    METHOD_NOT_ALLOWED_405(405, "不支持的HTTP方法"),
    INTERNAL_SERVER_ERROR_500(500, "服务器内部错误"),

    // 用户名密码错误,
    ILLEGAL_ARGUMENT_ERROR(1002, "参数错误:{}"),

    URI_PARSE_ERROR(1003, "uri解析错误"),
    HTTP_METHOD_PARSE_ERROR(1004, "http method 解析错误"),
    TOKEN_EXPIRED(1008, "token 已过期"),
    TOKEN_ILLEGAL(1009, "token 错误"),
    CONFIG_ERROR(1010, "配置错误"),
    DIR_NOT_FOUND(1011, "目录不存在[{}]"),
    DATE_TIME_ERROR(1012, "时间格式错误"),
    ;

    private Integer status;

    private final String msg;

    StatusMessageEnum(Integer status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    private StatusMessageEnum setStatus(Integer status) {
        this.status = status;
        return this;
    }

    public static StatusMessageEnum getByCode(Integer status) {
        for (StatusMessageEnum errorCode : values()) {
            if (errorCode.status.equals(status)) {
                return errorCode;
            }
        }
        return null;
    }

}
