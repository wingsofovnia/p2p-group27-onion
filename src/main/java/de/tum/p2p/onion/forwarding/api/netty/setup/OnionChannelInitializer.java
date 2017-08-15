package de.tum.p2p.onion.forwarding.api.netty.setup;

import de.tum.p2p.onion.forwarding.OnionForwarder;
import de.tum.p2p.onion.forwarding.api.netty.handler.*;
import de.tum.p2p.proto.message.Message;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class OnionChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int FRAME_LENGTH_PREFIX_LENGTH = Message.LENGTH_PREFIX_BYTES;
    private OnionForwarder forwarder;

    public OnionChannelInitializer(OnionForwarder forwarder) {
        this.forwarder = forwarder;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        log.trace("Initialize Pipeline");
        val pipe = ch.pipeline();
        pipe.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0, FRAME_LENGTH_PREFIX_LENGTH, -FRAME_LENGTH_PREFIX_LENGTH, FRAME_LENGTH_PREFIX_LENGTH, true));
        pipe.addLast("Message Discriminator", new OnionMessageDescriminatorHandler(forwarder));

        // Add handlers for API calls here
        log.trace("Adding Pipelines Handlers");
        {
            pipe.addLast("OnionCover", new OnionCoverMessageHandler(forwarder));
            pipe.addLast("OnionTunnelBuild", new OnionTunnelBuildMessageHandler(forwarder));
            pipe.addLast("OnionTunnelData", new OnionTunnelDataMessageHandler(forwarder));
            pipe.addLast("OnionTunnelIncoming", new OnionTunnelIncomingMessageHandler(forwarder));
            pipe.addLast("OnionTunnelDestroy", new OnionTunnelDestroyMessageHandler(forwarder));

        }
        log.trace("Finished Added Pipelines Handlers");

        pipe.addLast(new LengthFieldPrepender(FRAME_LENGTH_PREFIX_LENGTH, true));
        log.trace("Pipeline Initialized");
    }
}
