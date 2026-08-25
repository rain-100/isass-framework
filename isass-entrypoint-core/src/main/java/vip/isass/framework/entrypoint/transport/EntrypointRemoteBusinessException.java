// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.transport;

/**
 * 远程 Entrypoint 已接收请求并返回统一业务失败响应。
 */
public final class EntrypointRemoteBusinessException extends EntrypointTransportException {

    public EntrypointRemoteBusinessException(String message) {
        super(message, false);
    }
}
