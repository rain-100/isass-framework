// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.netty.tcp;

import vip.isass.framework.net.netty.channel.ChannelEventHandler;
import vip.isass.framework.net.netty.packet.TcpPacket;
import vip.isass.framework.net.netty.request.Request;
import vip.isass.framework.net.netty.request.RequestManager;
import vip.isass.framework.net.core.session.ISessionService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * channel事件被触发时，执行此类对应的事件处理方法
 *
 * @author Rain
 */
@Slf4j
@ChannelHandler.Sharable
public class TcpChannelEventHandler extends ChannelInboundHandlerAdapter implements ChannelEventHandler {

    private final ISessionService sessionService;

    @Getter
    private final RequestManager requestManager;

    public TcpChannelEventHandler(ISessionService sessionService, RequestManager requestManager) {
        this.sessionService = sessionService;
        this.requestManager = requestManager;
    }

    @Override
    public ISessionService getSessionService() {
        return sessionService;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channelActive0(ctx);
    }

    /**
     * 在ChannelHandler的channelRead()方法中应当尽快释放handler以让当前的NIO线程尽快处理下一个请求，
     * 那么channelRead()应当只进行数据的解码处理，然后将解码后的数据“发送”给业务逻辑类进行处理。
     *
     * @param cx     ChannelHandlerContext
     * @param object Object
     */
    @Override
    public void channelRead(ChannelHandlerContext cx, Object object) {
        channelRead1(cx, (TcpPacket) object, Request.Protocol.TCP);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        ctx.fireUserEventTriggered(evt);
        userEventTriggered0(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        exceptionCaught0(ctx, cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channelInactive0(ctx);
    }

    public static void main(String[] args) {
        Map<String, Integer> map = new HashMap<>(5000000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            map.put(i + "", i);
        }
        System.out.println("put结束，耗时：" + (System.currentTimeMillis() - start) + " ms");
        System.out.println("map.size=" + map.size());

        start = System.currentTimeMillis();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (key.equals("2000")) {
                System.out.println(true);
            }
        }
        System.out.println("遍历结束，耗时：" + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        Integer integer = map.get("2000");
        System.out.println(integer);
        System.out.println("get，耗时：" + (System.currentTimeMillis() - start) + " ms");
    }

}