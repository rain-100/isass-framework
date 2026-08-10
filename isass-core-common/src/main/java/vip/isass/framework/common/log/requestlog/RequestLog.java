// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.log.requestlog;

import cn.hutool.core.date.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import tools.jackson.databind.JsonNode;
import vip.isass.framework.common.support.JsonUtil;

import java.util.Date;

/**
 * copy from vip.isass.log.api.model.entity.RequestLog
 *
 * @author Rain
 */
@Getter
@Setter
@Accessors(chain = true)
public class RequestLog {

    /**
     * <p>
     * 标题
     * </p>
     * 数据库字段名: title
     * 数据库字段类型: varchar(128)
     */
    private String title;

    /**
     * <p>
     * 用户id
     * </p>
     * 数据库字段名: user_id
     * 数据库字段类型: varchar(64)
     */
    private String userId;

    /**
     * <p>
     * 用户昵称
     * </p>
     * 数据库字段名: nick_name
     * 数据库字段类型: varchar(64)
     */
    private String nickName;

    /**
     * <p>
     * 请求地址
     * </p>
     * 数据库字段名: uri
     * 数据库字段类型: varchar(512)
     */
    private String uri;

    /**
     * <p>
     * http方法
     * </p>
     * 数据库字段名: method
     * 数据库字段类型: varchar(16)
     */
    private String method;

    /**
     * <p>
     * 请求头
     * </p>
     * 数据库字段名: request_header
     * 数据库字段类型: json
     */
    private JsonNode requestHeader;

    /**
     * <p>
     * 响应头
     * </p>
     * 数据库字段名: response_header
     * 数据库字段类型: json
     */
    private JsonNode responseHeader;

    /**
     * <p>
     * 请求参数
     * </p>
     * 数据库字段名: request_param
     * 数据库字段类型: varchar(512)
     */
    private String requestParam;

    /**
     * <p>
     * 响应内容
     * </p>
     * 数据库字段名: response_content
     * 数据库字段类型: varchar(512)
     */
    private String responseContent;

    /**
     * <p>
     * 异常消息
     * </p>
     * 数据库字段名: exception_message
     * 数据库字段类型: varchar(255)
     */
    private String exceptionMessage;

    /**
     * <p>
     * 异常消息堆栈
     * </p>
     * 数据库字段名: exception_detail
     * 数据库字段类型: varchar(1024)
     */
    private String exceptionDetail;

    /**
     * <p>
     * 操作系统
     * </p>
     * 数据库字段名: os
     * 数据库字段类型: varchar(32)
     */
    private String os;

    /**
     * <p>
     * 浏览器
     * </p>
     * 数据库字段名: browser
     * 数据库字段类型: varchar(32)
     */
    private String browser;

    /**
     * <p>
     * 客户端ip
     * </p>
     * 数据库字段名: remote_addr
     * 数据库字段类型: varchar(64)
     */
    private String remoteAddr;

    /**
     * <p>
     * 请求时间
     * </p>
     * 数据库字段名: request_time
     * 数据库字段类型: bigint(20)
     */
    private Long requestTime;

    /**
     * <p>
     * 耗时
     * </p>
     * 数据库字段名: cost
     * 数据库字段类型: int(11)
     */
    private Integer cost;

    /**
     * <p>
     * 租户id
     * </p>
     * 数据库字段名: tenant_id
     * 数据库字段类型: varchar(64)
     */
    private String tenantId;

    /**
     * <p>
     * 应用id
     * </p>
     * 数据库字段名: app_id
     * 数据库字段类型: varchar(64)
     */
    private String appId;

    /**
     * <p>
     * 微服务名
     * </p>
     * 数据库字段名: service_name
     * 数据库字段类型: varchar(32)
     */
    private String serviceName;

    /**
     * <p>
     * 国密-完整性校验字符
     * </p>
     * 数据库字段名: gm_hash_value
     * 数据库字段类型: varchar(64)
     */
    private String gmHashValue;

    /**
     * <p>
     * 国密-初始化向量
     * </p>
     * 数据库字段名: iv
     * 数据库字段类型: varchar(64)
     */
    private String iv;

    /**
     * <p>
     * 加密服务类型
     * </p>
     * 数据库字段名: encryption_server_type
     * 数据库字段类型: int(11)
     */
    private Integer encryptionServerType;

    @Override
    @SneakyThrows
    public String toString() {
        return "RequestLog{" +
                "title='" + title + '\'' +
                ", userId='" + userId + '\'' +
                ", nickName='" + nickName + '\'' +
                ", uri='" + uri + '\'' +
                ", method='" + method + '\'' +
                ", requestHeader=" + JsonUtil.DEFAULT_INSTANCE.writeValueAsString(requestHeader) +
                ", responseHeader=" + JsonUtil.DEFAULT_INSTANCE.writeValueAsString(responseHeader) +
                ", requestParam='" + requestParam + '\'' +
                ", responseContent='" + responseContent + '\'' +
                ", exceptionMessage='" + exceptionMessage + '\'' +
                ", exceptionDetail='" + exceptionDetail + '\'' +
                ", os='" + os + '\'' +
                ", browser='" + browser + '\'' +
                ", remoteAddr='" + remoteAddr + '\'' +
                ", requestTime=" + DateUtil.formatDateTime(new Date(requestTime)) +
                ", cost=" + cost +
                ", tenantId='" + tenantId + '\'' +
                ", appId='" + appId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
