package de.tum.p2p.onion.forwarding.api;

import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelDataMessage;
import de.tum.p2p.proto.message.onion.forwarding.api.OnionTunnelIncomingMessage;
import de.tum.p2p.onion.forwarding.api.netty.setup.OnionChannelInitializer;
import de.tum.p2p.onion.forwarding.OnionForwarder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by micro on 30/06/2017.
 */

@Slf4j
public class OnionForwarderAPI {

    private OnionForwarder forwarder;
    private int listenPort;

    public OnionForwarderAPI(OnionForwarder forwarder, int listenPort) {
        this.forwarder = forwarder;
        this.listenPort = listenPort;
    }

    public void run() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new OnionChannelInitializer(forwarder))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            int port = listenPort;
            log.info(" - Onion Server Channel has been initialized on /127.0.0.1:" + port);

            var channel = b.bind(port).sync().channel().closeFuture().sync().channel();

            forwarder.addIncomingTunnelObserver((tunnelId) -> channel.writeAndFlush(new OnionTunnelIncomingMessage(tunnelId)));
            forwarder.addIncomingDataObserver((tunnelId, byteBuffer) -> channel.writeAndFlush(new OnionTunnelDataMessage(tunnelId, byteBuffer)));
        }
        finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
