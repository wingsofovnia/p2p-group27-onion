package de.tum.p2p.onion.forwarding.api.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelDataMessage;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class OnionTunnelDataMessageHandler extends SimpleChannelInboundHandler<OnionTunnelDataMessage> {

    private OnionForwarder onionForwarder;

    public OnionTunnelDataMessageHandler(OnionForwarder onionForwarder) {
        this.onionForwarder = onionForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OnionTunnelDataMessage msg) throws Exception {
        onionForwarder.forward(msg.getTunnelId(), msg.getData());
    }
}
