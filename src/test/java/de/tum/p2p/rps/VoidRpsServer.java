package de.tum.p2p.rps;

import de.tum.p2p.Peer;
import de.tum.p2p.Peers;
import de.tum.p2p.voidphone.rps.api.RpsPeerMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.val;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static de.tum.p2p.Peers.randPeer;
import static de.tum.p2p.VoidApiMessages.toByteBuf;

/**
 * Issues random peer from given list or generate random peers via
 * {@link Peers#randPeer()}. This test imp DOESN'T USE GOSSIP MODULE
 * and uses message definitions from voidphone testing suite.
 */
public class VoidRpsServer implements Closeable {

    private final List<Peer> peerDatabase = new ArrayList<>();

    private Channel channel;

    public VoidRpsServer(InetAddress inetAddress, int port) {
        this(inetAddress, port, null);
    }

    public VoidRpsServer(InetAddress inetAddress, int port, List<Peer> peers) {
        this.channel = buildServerChannel(inetAddress, port);

        if (peers != null)
            peerDatabase.addAll(peers);
    }

    private Channel buildServerChannel(InetAddress inetAddress, int port) {
        val bossGroup = new NioEventLoopGroup();
        val workerGroup = new NioEventLoopGroup();

        val b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                val peer = fetchPeer();
                                val peerSocketAddress = new InetSocketAddress(peer.address(), peer.port());

                                val rpsPeerMessage = new RpsPeerMessage(peerSocketAddress, (RSAPublicKey) peer.publicKey());
                                ctx.channel().writeAndFlush(toByteBuf(rpsPeerMessage));
                            }
                        });
                }
            });

        return b.bind(inetAddress, port).syncUninterruptibly().channel();
    }

    private Peer fetchPeer() {
        if (!peerDatabase.isEmpty()) {
            Collections.shuffle(peerDatabase);
            return peerDatabase.get(0);
        }

        return randPeer();
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.disconnect();
            this.channel.close().syncUninterruptibly();
        } catch (Exception e) {
            throw new IOException("Failed to close channel", e);
        }
    }
}
