// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.proxy.service.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.isass.framework.common.web.Resp;
import vip.isass.framework.net.core.message.Message;
import vip.isass.framework.net.core.session.ISessionService;
import vip.isass.framework.net.core.session.SessionBindingInfoChangeReq;

import java.util.Collection;
import java.util.Map;

/**
 * @author rain
 */
@Slf4j
@RestController
@RequestMapping("/${spring.application.name}/session")
public class NetSessionController {

    private final ISessionService sessionService;

    public NetSessionController(ISessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/send")
    public Resp<?> sendMessage(@RequestBody Message message) {
        sessionService.sendMessage(message);
        return Resp.bizSuccess();
    }

    @PostMapping("/send/batch")
    public Resp<?> sendMessages(@RequestBody Collection<Message> messages) {
        sessionService.sendMessages(messages);
        return Resp.bizSuccess();
    }

    @PostMapping("/info")
    public Resp<?> saveSessionInfo(@RequestBody SessionBindingInfoChangeReq req) {
        if (StrUtil.isNotBlank(req.getSessionId())) {
            if (StrUtil.isNotBlank(req.getResetUserId())) {
                sessionService.setUserId(req.getSessionId(), req.getResetUserId());
            } else if (Boolean.TRUE.equals(req.getRemoveUserId())) {
                sessionService.removeUserId(req.getSessionId());
            }

            if (StrUtil.isNotBlank(req.getAlias())) {
                sessionService.setAlias(req.getSessionId(), req.getAlias());
            } else if (Boolean.TRUE.equals(req.getRemoveAlias())) {
                sessionService.removeAlias(req.getSessionId());
            }

            if (CollUtil.isNotEmpty(req.getTags())) {
                sessionService.setTags(req.getSessionId(), req.getTags());
            } else if (Boolean.TRUE.equals(req.getRemoveAllTags())) {
                sessionService.removeTags(req.getSessionId());
            } else {
                if (CollUtil.isNotEmpty(req.getAddTags())) {
                    sessionService.addTags(req.getSessionId(), req.getAddTags());
                }
                if (CollUtil.isNotEmpty(req.getRemoveTags())) {
                    sessionService.removeTags(req.getSessionId(), req.getRemoveTags());
                }
            }
            return Resp.bizSuccess();
        }

        Assert.notBlank(req.getUserId(), "sessionId, userId必填其一");
        if (CollUtil.isNotEmpty(req.getTags())) {
            sessionService.setTagsByUserId(req.getUserId(), req.getTags());
        } else {
            if (CollUtil.isNotEmpty(req.getAddTags())) {
                sessionService.addTagsByUserId(req.getUserId(), req.getAddTags());
            }
            if (CollUtil.isNotEmpty(req.getRemoveTags())) {
                sessionService.removeTagsByUserId(req.getUserId(), req.getRemoveTags());
            }

        }
        return Resp.bizSuccess();
    }

    // region user id

    /**
     * 获取用户 id
     *
     * @return 用户 id
     */
    @GetMapping("/{sessionId}/userId")
    public Resp<String> getUserId(@PathVariable("sessionId") String sessionId) {
        return Resp.bizSuccess(sessionService.getUserId(sessionId));
    }

    @PostMapping("/user/isOnline")
    public Resp<Map<String, Boolean>> isOnline(@RequestBody Collection<String> userIds) {
        return Resp.bizSuccess(sessionService.isOnline(userIds));
    }

    // endregion


    // region alias

    /**
     * 获取别名
     *
     * @return 别名
     */
    @GetMapping("/{sessionId}/alias")
    public Resp<String> getAlias(@PathVariable("sessionId") String sessionId) {
        return Resp.bizSuccess(sessionService.getAlias(sessionId));
    }

    /**
     * 获取别名
     *
     * @param alias 别名
     * @return 会话 id 列表
     */
    @GetMapping("/sessionIds")
    public Resp<Collection<String>> findSessionIdsByAlias(@RequestParam("alias") String alias) {
        return Resp.bizSuccess(sessionService.findSessionIdsByAlias(alias));
    }

    /**
     * 查询已有别名
     *
     * @return 查询已有别名
     */
    @GetMapping("/alias")
    public Resp<Collection<String>> findAliases(@RequestParam(name = "prefix", required = false) String prefix) {
        return Resp.bizSuccess(sessionService.findAliases(prefix));
    }

    // endregion

    // region tag

    /**
     * 获取标签
     *
     * @return 标签列表
     */
    @GetMapping("/{sessionId}/tags")
    public Resp<Collection<String>> getTags(@PathVariable("sessionId") String sessionId) {
        return Resp.bizSuccess(sessionService.findTags(sessionId));
    }

    /**
     * 根据用户获取标签
     *
     * @return 标签列表
     */
    @GetMapping("/tags/{userId}")
    public Resp<Collection<String>> getTagsByUserId(@PathVariable("userId") String userId) {
        return Resp.bizSuccess(sessionService.findTagsByUserId(userId));
    }

    /**
     * 根据标签查找会话
     *
     * @param tags 标签集合
     * @return 符合条件的会话集合
     */
    @GetMapping("/any")
    public Resp<Collection<String>> findSessionsByAnyMatchTags(@RequestParam("tags") Collection<String> tags) {
        return Resp.bizSuccess(sessionService.findSessionsByAnyMatchTags(tags));
    }

    /**
     * 判断会话是否拥有任意给定的标签
     *
     * @param sessionId 会话 id
     * @param tags      给定的标签
     * @return 是否拥有标签
     */
    @GetMapping("/{sessionId}/containAnyTag")
    public Resp<Boolean> containAnyTag(@PathVariable("sessionId") String sessionId, @RequestParam("tags") Collection<String> tags) {
        return Resp.bizSuccess(sessionService.containAnyTag(sessionId, tags));
    }

    /**
     * 判断会话是否拥有所有给定的标签
     *
     * @param sessionId 会话 id
     * @param tags      给定的标签
     * @return 是否拥有标签
     */
    @GetMapping("/{sessionId}/containTags")
    public Resp<Boolean> containAllTags(@PathVariable("sessionId") String sessionId, @RequestParam("tags") Collection<String> tags) {
        return Resp.bizSuccess(sessionService.containAllTags(sessionId, tags));
    }

    // endregion

}

