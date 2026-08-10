// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.packet.impl.coder;

import cn.hutool.core.util.StrUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Scope;
import vip.isass.framework.net.netty.packet.Decoder;
import vip.isass.framework.net.netty.packet.IPacket;
import vip.isass.framework.net.netty.packet.TcpPacket;

import java.util.List;

/**
 * 收到客户端的数据后，执行此类进行数据解码
 *
 * @author Rain
 */
@Slf4j
@ConditionalOnMissingBean(Decoder.class)
@Scope("prototype")
public class IsassBinaryPacketDecoder extends Decoder {

    /**
     * 一个完整的报文包最大为 100M
     */
    private static final int MAX_MESSAGE_BYTES = 100 * 1024 * 1024;

    /**
     * tcp数据包的报文结构:
     * header: fullLength(4B) + serializeMode(4B) + cmdLength(4B) + cmd
     * payload: &lt; 100M - 3 * 4B
     * total: 100M
     */
    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readableBytes = in.readableBytes();

        log.debug("收到网络包，长度：{} 字节", readableBytes);

        if (readableBytes > MAX_MESSAGE_BYTES) {
            throw new IllegalArgumentException(StrUtil.format(
                "网络包的可读数据长度大于一个网络包(50m)长度：{}", readableBytes));
        }

        // 获取网络包的前4个字节，作为一个完整报文包的数据长度
        if (readableBytes < Integer.BYTES) {
            return;
        }

        in.markReaderIndex();

        // 整个包的长度（字节）
        int fullLength = in.readInt();
        if (fullLength < 0) {
            log.error("tcp数据包的length值为负数[{}], 将强制关闭此tcp链接！{}", fullLength, ctx);
            ctx.close();
            return;
        }

        // 可读字节数小于整包的长度，说明数据未完全接收。返回，等待下次读取
        if (readableBytes < fullLength) {
            in.resetReaderIndex();
            return;
        }

        IPacket packet = createPackage(ctx, in, fullLength);
        out.add(packet);

        log.debug("已拆包长度：{}", readableBytes - in.readableBytes());
    }

    @SneakyThrows
    private IPacket createPackage(ChannelHandlerContext ctx, ByteBuf in, int fullLength) {
        TcpPacket packet = TcpPacket.builder()
            .fullLength(fullLength)
            .serializeMode(in.readInt())
            .cmdLength(in.readInt())
            .build();

        if (packet.getCmdLength() < 0) {
            throw new RuntimeException("tcp 报文的 cmdLength 不能为负数");
        }

        if (packet.getCmdLength() > 0) {
            byte[] bytes = new byte[packet.getCmdLength()];
            in.readBytes(bytes);
            packet.setCmd(new String(bytes));
        }

        int contentLength = fullLength - 16;
        if (contentLength == 0) {
            return packet;
        }

        byte[] bytes = new byte[contentLength];
        in.readBytes(bytes);
        packet.setPayload(bytes);
        return packet;
    }

}
