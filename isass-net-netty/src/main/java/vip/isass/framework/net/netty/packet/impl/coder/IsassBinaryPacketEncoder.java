// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.packet.impl.coder;

import com.google.protobuf.GeneratedMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.EmptyArrays;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import vip.isass.framework.net.netty.packet.Encoder;
import vip.isass.framework.net.netty.packet.TcpPacket;
import vip.isass.framework.common.serialization.SerializeMode;
import vip.isass.framework.common.support.JsonUtil;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 发送数据给客户端的时，执行此类进行编码
 *
 * @author Rain
 */
@Slf4j
@ChannelHandler.Sharable
@ConditionalOnMissingBean(Encoder.class)
public class IsassBinaryPacketEncoder extends Encoder<TcpPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, TcpPacket packet, ByteBuf out) {
        out.writeBytes(encode(packet));
    }

    @SneakyThrows
    public static ByteBuf encode(TcpPacket packet) {
        byte[] contentBytes;
        Object payload = packet.getPayload();
        if (payload == null) {
            contentBytes = EmptyArrays.EMPTY_BYTES;
        } else if (payload instanceof byte[]) {
            contentBytes = (byte[]) packet.getPayload();
        } else if (SerializeMode.JSON.getCode().equals(packet.getSerializeMode())
            || packet.getSerializeMode() == null) {
            if (payload instanceof String) {
                contentBytes = (((String) payload).getBytes(UTF_8));
            } else {
                contentBytes = JsonUtil.DEFAULT_INSTANCE.writeValueAsBytes(payload);
            }
        } else if (SerializeMode.PROTOBUF2.getCode().equals(packet.getSerializeMode())) {
            if (payload instanceof GeneratedMessage) {
                contentBytes = ((GeneratedMessage) payload).toByteArray();
            } else {
                throw new IllegalArgumentException("序列化模式是pb, 但content不是GeneratedMessage");
            }
        } else {
            throw new UnsupportedOperationException("编码器不支持的序列化类型:" + packet.getSerializeMode());
        }

        packet.setFullLength(12 + contentBytes.length);
        ByteBuf byteBuf = Unpooled.buffer()
            .writeInt(packet.getFullLength())
            .writeInt(packet.getSerializeMode() == null ? SerializeMode.JSON.getCode() : packet.getSerializeMode())
            .writeInt(packet.getCmdLength());

        if (packet.getCmd() != null) {
            byteBuf.writeBytes(packet.getCmd().getBytes(UTF_8));
        }
        byteBuf.writeBytes(contentBytes);
        log.debug("编码后字节大小:{} 字节", byteBuf.readableBytes());
        return byteBuf;
    }

}
