package de.tum.p2p.onion.forwarding.netty.handler;

import com.google.common.eventbus.EventBus;
import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.onion.forwarding.netty.event.TunnelCoverReceived;
import de.tum.p2p.onion.forwarding.netty.event.TunnelDatumReceived;
import de.tum.p2p.proto.message.onion.forwarding.composite.TunnelDatum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * {@code TunnelConnectHandler} handles plaintext {@link TunnelDatum} received from
 * unwrapping {@code TunnelRelayMessage} by {@link TunnelRelayHandler}.
 * It checks whether the datum is cover or not and then notify {@code OnionForwarder}'s
 * data listeners.
 *
 * @author Illia Ovchynnikov &lt;illia.ovchynnikov@gmail.com&gt;
 */
@Slf4j
public class TunnelDatumHandler extends SimpleChannelInboundHandler<Pair<TunnelId, TunnelDatum>> {

    private final EventBus eventBus;

    public TunnelDatumHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Pair<TunnelId, TunnelDatum> tunnelIdDatum) throws Exception {
        val tunnelId = tunnelIdDatum.getKey();
        val datum = tunnelIdDatum.getValue();

        if (datum.isCover()) {
            eventBus.post(TunnelCoverReceived.from(tunnelId));
            log.debug("Listeners has been notified about incoming cover data");
        } else {
            eventBus.post(TunnelDatumReceived.of(tunnelId, ByteBuffer.wrap(datum.payload())));
            log.debug("Listeners has been notified about incoming data {}", Arrays.toString(datum.payload()));
        }
    }
}
