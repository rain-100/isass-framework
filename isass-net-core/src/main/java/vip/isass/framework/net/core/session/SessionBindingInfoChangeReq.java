// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Collection;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionBindingInfoChangeReq {

    /**
     * 根据 sessionId 修改信息。sessionId(优先)、userId 二选一
     */
    private String sessionId;

    /**
     * 根据 userId 修改信息
     */
    private String userId;

    /**
     * 重新设置会话绑定的用户，resetUserId(优先)、removeUserIdFlag 二选一
     * resetUserId 只能用在根据 sessionId 修改信息的情况
     */
    private String resetUserId;

    /**
     * 移除会话绑定的用户
     */
    private Boolean removeUserId;

    /**
     * 重新设置会话绑定的别名，resetAlias(优先)、removeAlias 二选一
     * resetAlias 只能用在根据 sessionId 修改信息的情况
     */
    private String alias;

    /**
     * 移除会话绑定的别名
     * removeAlias 只能用在根据 sessionId 修改信息的情况
     */
    private Boolean removeAlias;

    /**
     * 重新设置会话绑定的标签，resetTags(优先)、removeAllTags 二选一
     * resetAlias 只能用在根据 sessionId 修改信息的情况
     */
    private Collection<String> tags;

    private Boolean removeAllTags;

    /**
     * 添加标签，resetTags、removeAllTags 为空时生效
     */
    private Collection<String> addTags;

    /**
     * 需要删除的标签，resetTags、removeAllTags 为空时生效
     */
    private Collection<String> removeTags;


}
