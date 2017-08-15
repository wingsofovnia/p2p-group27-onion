package de.tum.p2p.onion.forwarding.api.netty.handler;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelBuildMessage;
import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelReadyMessage;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import de.tum.p2p.onion.forwarding.Tunnel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CompletableFuture;

public class OnionTunnelBuildMessageHandler extends SimpleChannelInboundHandler<OnionTunnelBuildMessage> {

    private OnionForwarder onionForwarder;

    public OnionTunnelBuildMessageHandler(OnionForwarder onionForwarder) {
        this.onionForwarder = onionForwarder;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, OnionTunnelBuildMessage msg) throws Exception {
        CompletableFuture<Tunnel> onionForwarderTunnel = onionForwarder.createTunnel(msg.getDestinationPeer());
        onionForwarderTunnel.thenAccept((tunnel) -> {
            ctx.channel().write(new OnionTunnelReadyMessage(tunnel.id(), tunnel.destinationKey()));
        });
    }
}
