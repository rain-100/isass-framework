// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web.header;

import java.net.URI;

/** 完整描述一次待发送请求，供需要签名的附加请求头提供者使用。 */
public record AdditionalRequestHeaderContext(
        String method,
        URI uri,
        byte[] body,
        boolean unsignedPayload
) {

    public AdditionalRequestHeaderContext {
        body = body == null ? new byte[0] : body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public AdditionalRequestHeaderContext(String method, URI uri, byte[] body) {
        this(method, uri, body, false);
    }
}
