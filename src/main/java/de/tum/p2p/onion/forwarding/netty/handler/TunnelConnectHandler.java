package de.tum.p2p.onion.forwarding.netty.handler;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.onion.forwarding.netty.channel.ClientChannelFactory;
import de.tum.p2p.onion.forwarding.netty.context.RoutingContext;
import de.tum.p2p.proto.message.onion.forwarding.TunnelExtendMessage;
import de.tum.p2p.proto.message.onion.forwarding.composite.TunnelConnect;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

/**
 * {@code TunnelConnectHandler} receives {@link TunnelConnect} payloads revealed
 * from {@code TunnelRelayMessage}, create a {@link TunnelExtendMessage}, connect
 * to the peer to be a new member of the tunnel and forwards the extend request.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelConnectHandler extends SimpleChannelInboundHandler<Pair<TunnelId, TunnelConnect>> {

    private final RoutingContext routingContext;
    private final ClientChannelFactory clientChannelFactory;

    public TunnelConnectHandler(RoutingContext routingContext, ClientChannelFactory clientChannelFactory) {
        this.routingContext = routingContext;
        this.clientChannelFactory = clientChannelFactory;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Pair<TunnelId, TunnelConnect> tunnelIdConnect)
            throws Exception {
        val tunnelId = tunnelIdConnect.getLeft();
        val connect = tunnelIdConnect.getRight();

        clientChannelFactory.connect(connect.socketDestination()).thenAccept(channel -> {
            routingContext.setNextHop(tunnelId, channel);

            val extendMsg = new TunnelExtendMessage(tunnelId, connect.requestId(),
                connect.sourceKey(), connect.handshake());

            channel.writeAndFlush(extendMsg);
            log.debug("ONION_TUNNEL_EXTEND confirmation with HS has been sent via tunnel {} from {} to {}",
                tunnelId, ctx.channel().localAddress(), ctx.channel().remoteAddress());
        });
    }
}
