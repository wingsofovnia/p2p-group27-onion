package de.tum.p2p.onion.forwarding.api.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelIncomingMessage;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class OnionTunnelIncomingMessageHandler extends SimpleChannelInboundHandler<OnionTunnelIncomingMessage> {

    private OnionForwarder onionForwarder;

    public OnionTunnelIncomingMessageHandler(OnionForwarder onionForwarder) {
        this.onionForwarder = onionForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OnionTunnelIncomingMessage msg) throws Exception {

        onionForwarder.addIncomingTunnelObserver((tunnelId) -> {
            ctx.writeAndFlush(new OnionTunnelIncomingMessage(tunnelId));
        });
    }
}
