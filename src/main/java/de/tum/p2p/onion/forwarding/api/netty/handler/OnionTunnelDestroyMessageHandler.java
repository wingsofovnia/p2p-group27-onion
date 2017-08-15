package de.tum.p2p.onion.forwarding.api.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelDestroyMessage;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class OnionTunnelDestroyMessageHandler extends SimpleChannelInboundHandler<OnionTunnelDestroyMessage> {

    private OnionForwarder onionForwarder;

    public OnionTunnelDestroyMessageHandler(OnionForwarder onionForwarder) {
        this.onionForwarder = onionForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OnionTunnelDestroyMessage msg) throws Exception {
        onionForwarder.destroyTunnel(msg.getTunnelId());
    }
}
