package de.tum.p2p.rps.remote.netty;

import de.tum.p2p.Peer;
import de.tum.p2p.onion.forwarding.OnionInitializationException;
import de.tum.p2p.proto.message.Message;
import de.tum.p2p.proto.message.rps.RpsPeerMessage;
import de.tum.p2p.proto.message.rps.RpsQueryMessage;
import de.tum.p2p.rps.PeerSamplingException;
import de.tum.p2p.rps.RandomPeerSampler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static de.tum.p2p.util.netty.ByteBufs.safeContent;

/**
 * The {@code NettyRemoteRandomPeerSampler} implementation acts like a
 * proxy for a remote {@link RandomPeerSampler}.
 * <p>
 * The comminication with the remote RPS is done via TCP connection
 * using {@link de.tum.p2p.proto.message.MessageType} 540-541.
 */
public class NettyRemoteRandomPeerSampler implements RandomPeerSampler {

    private static final Logger log = LoggerFactory.getLogger(NettyRemoteRandomPeerSampler.class);

    private static final int FRAME_LENGTH_PREFIX_LENGTH = Message.LENGTH_PREFIX_BYTES;

    private static final int INCOMING_RANDOM_PEERS_QUEUE_CAP = 16;

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private final BlockingQueue<RpsPeerMessage> incomingRandPeerMessages
        = new ArrayBlockingQueue<>(INCOMING_RANDOM_PEERS_QUEUE_CAP);

    private final Channel channel;

    private final Duration samplingTimeout;

    private NettyRemoteRandomPeerSampler(Builder builder) {
        this.channel = buildRemoteRpsChannel(builder.inetAddress, builder.port, builder.channel,
            builder.eventLoopExecutors, builder.channelOptions, builder.loggerLevel);

        this.samplingTimeout = builder.samplingTimeout;
    }

    private Channel buildRemoteRpsChannel(InetAddress inetAddress, Integer port,
                                       Class<? extends Channel> channel, EventLoopGroup eventExecutors,
                                       Map<ChannelOption, Object> opts, LogLevel logLevel) {
        log.debug("Initializing remote RPS channel on {}:{}, channel {}, opts {}, logger level {}",
            inetAddress.getHostAddress(), port, channel.getName(), opts.toString(), logLevel);

        val b = new Bootstrap();

        b.group(eventExecutors)
            .channel(channel);
        opts.forEach(b::option);

        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                val pipe = ch.pipeline();

                if (logLevel != null)
                    pipe.addLast(new LoggingHandler(logLevel));

                // ->O Input decoding & processing
                pipe.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0,
                    FRAME_LENGTH_PREFIX_LENGTH, -FRAME_LENGTH_PREFIX_LENGTH, FRAME_LENGTH_PREFIX_LENGTH, true));

                // Adds RPS PEER responses to the queue
                pipe.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                        incomingRandPeerMessages.offer(RpsPeerMessage.fromBytes(safeContent(msg)));
                    }
                });

                // <-O Output encoding
                pipe.addLast(new LengthFieldPrepender(FRAME_LENGTH_PREFIX_LENGTH, true));
            }
        });

        try {
            val chl = b.connect(inetAddress, port).syncUninterruptibly().channel();

            log.info("Remote RPS channel set up on {}:{}", inetAddress.getHostAddress(), port);
            return chl;
        } catch (Exception e) {
            throw new OnionInitializationException("Failed to init remote RPS channel", e);
        }
    }

    @Override
    public CompletableFuture<Peer> sample() throws PeerSamplingException {
        val peerFuture = new CompletableFuture<Peer>();

        val randPeerRequestFuture = channel.writeAndFlush(Unpooled.wrappedBuffer(RpsQueryMessage.me().bytes()));
        randPeerRequestFuture.addListener(future -> {
            if (!future.isSuccess())
                throw new PeerSamplingException("Failed to get a response from remote RPS");

            ForkJoinPool.commonPool().execute(() -> {
                try {
                    val peerMsg = incomingRandPeerMessages.poll(samplingTimeout.toMillis(), TimeUnit.MILLISECONDS);
                    if (peerMsg == null)
                        throw new PeerSamplingException("Server didn't respond with a random peer in defined timeout");

                    peerFuture.complete(parsePeer(peerMsg));
                } catch (Exception e) {
                    peerFuture.completeExceptionally(e);
                }
            });
        });

        return peerFuture;
    }

    private static Peer parsePeer(RpsPeerMessage message) {
        return Peer.of(message.inetAddress(), message.port(), message.hostkey());
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.disconnect();
            this.channel.close().syncUninterruptibly();
        } catch (Exception e) {
            log.error("Failed to close remote RPS channel", e);
            throw new IOException("Failed to close remote RPS channel", e);
        }
    }

    public static final class Builder {

        private static final Duration DEFAULT_SAMPLING_TIMEOUT = Duration.ofSeconds(2);

        private static final String DEFAULT_HOSTKEY_ALG = "RSA";

        private EventLoopGroup eventLoopExecutors;

        private Class<? extends Channel> channel;

        private Map<ChannelOption, Object> channelOptions;

        private InetAddress inetAddress;

        private int port;

        private String hostKeyAlg;

        private Duration samplingTimeout;

        private LogLevel loggerLevel;

        public Builder() {
            this.eventLoopExecutors = new NioEventLoopGroup();

            this.channel = NioSocketChannel.class;
            this.channelOptions = new HashMap<ChannelOption, Object>() {{
                put(ChannelOption.SO_KEEPALIVE, true);
            }};

            try {
                this.inetAddress = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                // ignore (since it's just a default value
            }

            this.samplingTimeout = DEFAULT_SAMPLING_TIMEOUT;
            this.hostKeyAlg = DEFAULT_HOSTKEY_ALG;
        }

        public Builder eventLoopExecutors(EventLoopGroup serverExecutors) {
            this.eventLoopExecutors = serverExecutors;
            return this;
        }

        public Builder channel(Class<? extends Channel> channel) {
            this.channel = channel;
            return this;
        }

        public Builder channelOptions(ChannelOption option, Object value) {
            this.channelOptions.put(option, value);
            return this;
        }

        public Builder inetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        public Builder hostKeyAlg(String hostKeyAlg) {
            this.hostKeyAlg = hostKeyAlg;
            return this;
        }

        public Builder samplingTimeout(Duration samplingTimeout) {
            this.samplingTimeout = samplingTimeout;
            return this;
        }

        public NettyRemoteRandomPeerSampler build() {
            return new NettyRemoteRandomPeerSampler(this);
        }
    }
}
