package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.forwarding.OnionTunnelingException;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.TunnelRetireMessage;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.val;

/**
 * {@code TunnelRetireHandler} handles {@link TunnelRetireMessage} messages
 * and clears {@link RoutingContext} from routes assigned with {@code TunnelId}
 * given in {@code TunnelRetireMessage}.
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public class TunnelRetireHandler extends SimpleChannelInboundHandler<TunnelRetireMessage> {

    private final RoutingContext routingContext;

    public TunnelRetireHandler(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TunnelRetireMessage tunnelRetireMsg) throws Exception {
        val tunnelId = tunnelRetireMsg.tunnelId();

        if (routingContext.hasNextHop(tunnelId)) {
            routingContext.nextHop(tunnelId).writeAndFlush(tunnelRetireMsg)
                .addListener((ChannelFutureListener) transfer -> {
                    if (!transfer.isSuccess())
                        throw new OnionTunnelingException("Failed propagate request " +
                            "to retire the tunnel " + tunnelId, transfer.cause());
                });
        }

        routingContext.forget(tunnelId);
    }
}
