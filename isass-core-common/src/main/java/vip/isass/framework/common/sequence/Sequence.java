// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.sequence;


import vip.isass.framework.common.support.Support;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 获取序列
 *
 * @param <T> 序列类型
 * @author Rain
 */
public interface Sequence<T> extends Support {

    /**
     * @return 序列
     */
    T next();

    /**
     * 把稳定业务身份确定性映射为正数 long ID。
     *
     * <p>算法固定为：使用 UTF-8 对身份编码，计算 SHA-256，按大端序读取摘要前八个字节，
     * 清除符号位，并把零映射为一。该算法属于持久化数据协议，不能在不迁移历史 ID 的情况下修改。</p>
     *
     * <p>业务身份必须包含调用方自己的命名空间，例如 {@code auth-resource:...}；哈希映射
     * 不能从数学上保证无碰撞，调用方仍需使用唯一索引或固定 ID 内容校验处理冲突。</p>
     *
     * @param identity 带业务命名空间的稳定身份
     * @return {@code 1..Long.MAX_VALUE} 范围内的稳定 ID
     */
    static long stableLongId(String identity) {
        if (identity == null || identity.isBlank()) {
            throw new IllegalArgumentException("稳定 ID 身份必填");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            long value = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
            return value == 0 ? 1 : value;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

}
