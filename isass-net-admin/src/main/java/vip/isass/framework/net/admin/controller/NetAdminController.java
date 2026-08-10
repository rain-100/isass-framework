// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.admin.controller;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.admin.model.vo.OnlineEquipmentVo;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.SessionInfoCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author rain
 */
@Slf4j
@RestController
@RequestMapping("/${spring.application.name}/net/admin/session")
public class NetAdminController {

    private final ISessionService sessionService;

    public NetAdminController(ISessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 获取所有会话信息(用于调试)
     *
     * @return 会话信息集合
     */
    @GetMapping("/sessionInfoCollection")
    public Resp<SessionInfoCollection> getSessionInfoCollection() {
        return Resp.bizSuccess(sessionService.getSessionInfoCollection());
    }

    /**
     * 获取所有在线设备
     *
     * @return 在线设备 id 集合
     */
    @GetMapping("/equipment/online")
    public Resp<Collection<OnlineEquipmentVo>> findOnlineEquipments() {
        Collection<String> equipmentAliases = sessionService.findAliases("equipmentId:");
        if (CollUtil.isEmpty(equipmentAliases)) {
            return Resp.bizSuccess(Collections.emptyList());
        }

        List<OnlineEquipmentVo> onlineEquipments = new ArrayList<>();

        // 根据 alias 获取 userId
        for (String equipmentAlias : equipmentAliases) {
            Collection<String> sessionIds = sessionService.findSessionIdsByAlias(equipmentAlias);
            if (CollUtil.isEmpty(sessionIds)) {
                onlineEquipments.add(OnlineEquipmentVo.builder()
                        .equipmentId(equipmentAlias.replace("equipmentId:", ""))
                        .build());
                continue;
            }

            // 根据 sessionIds 获取每个 sessionId 对应的 userId
            for (String sessionId : sessionIds) {
                String userId = sessionService.getUserId(sessionId);
                onlineEquipments.add(OnlineEquipmentVo.builder()
                        .equipmentId(equipmentAlias.replace("equipmentId:", ""))
                        .userId(userId)
                        .build());
            }
        }

        return Resp.bizSuccess(onlineEquipments);
    }
}

