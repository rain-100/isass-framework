// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.socketio;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author Rain
 */
@Controller
public class SocketioForwardController {

    @GetMapping("/${spring.application.name}/socketio.html")
    public String forwardSocketIoHtml() {
        return "forward:/socketio.html";
    }

    @GetMapping("/${spring.application.name}/socket.io.js")
    public String forwardSocketIoJs() {
        return "forward:/socket.io.js";
    }

    @GetMapping("/${spring.application.name}/jquery/3.3.1/jquery.min.js")
    public String forwardJquery() {
        return "forward:/jquery/3.3.1/jquery.min.js";
    }

}
