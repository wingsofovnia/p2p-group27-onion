package de.tum.p2p.onion.forwarding.netty.handler.server;

import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.Router;
import de.tum.p2p.proto.message.onion.forwarding.TunnelRetireMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.val;

public class TunnelRetireHandler extends SimpleChannelInboundHandler<TunnelRetireMessage> {

    private final Router router;

    public TunnelRetireHandler(Router router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelRetireMessage tunnelRetireMsg) throws Exception {
        val tunnelId = tunnelRetireMsg.tunnelId();

        router.routeNext(tunnelId).ifPresent(channel ->
            channel.writeAndFlush(tunnelRetireMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed propagate request " +
                            "to retire the tunnel " + tunnelId, transfer.cause());

                    router.forget(tunnelId);
                })
        );
    }
}
