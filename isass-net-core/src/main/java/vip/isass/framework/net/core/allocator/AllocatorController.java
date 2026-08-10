// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.allocator;

import cn.hutool.extra.servlet.JakartaServletUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping
public class AllocatorController {

    private final AllocatorService allocatorService;

    public AllocatorController(AllocatorService allocatorService) {
        this.allocatorService = allocatorService;
    }

    @GetMapping("/{serverName}/allocator/node")
    public Resp<String> allocate(HttpServletRequest request,
                                 @PathVariable("serverName") String serverName) {
        String clientIp = JakartaServletUtil.getClientIP(request);
        return Resp.bizSuccess(allocatorService.allocateAccessUrl(serverName, clientIp));
    }

}
