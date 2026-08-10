// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support.okhttp;

import okhttp3.WebSocket;

public interface OnSocketConnectedHandler {

    void handle(WebSocket webSocket);

}
