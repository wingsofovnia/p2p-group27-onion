package de.tum.p2p.onion.forwarding.api.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionCoverMessage;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class OnionCoverMessageHandler extends SimpleChannelInboundHandler<OnionCoverMessage> {

    private OnionForwarder onionForwarder;

    public OnionCoverMessageHandler(OnionForwarder onionForwarder) {
        this.onionForwarder = onionForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OnionCoverMessage msg) throws Exception {
        onionForwarder.cover(msg.size());
    }
}
