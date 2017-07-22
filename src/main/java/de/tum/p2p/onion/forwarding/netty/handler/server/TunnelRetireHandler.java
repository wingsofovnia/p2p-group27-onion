package de.tum.p2p.onion.forwarding.netty.handler.server;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.forwarding.netty.TunnelRouter;
import de.tum.p2p.onion.forwarding.netty.event.TunnelRetireCommand;
import de.tum.p2p.proto.message.onion.forwarding.TunnelRetireMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.val;

public class TunnelRetireHandler extends SimpleChannelInboundHandler<TunnelRetireMessage> {

    private final TunnelRouter tunnelRouter;
    private final EventBus eventBus;

    public TunnelRetireHandler(TunnelRouter tunnelRouter, EventBus eventBus) {
        this.tunnelRouter = tunnelRouter;
        this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelRetireMessage tunnelRetireMsg) throws Exception {
        val tunnelId = tunnelRetireMsg.tunnelId();

        // Propagate retire command to down the tunnel
        tunnelRouter.resolveNext(tunnelId).ifPresent(channel -> channel.writeAndFlush(tunnelRetireMsg));

        eventBus.post(TunnelRetireCommand.of(tunnelId));
    }
}
