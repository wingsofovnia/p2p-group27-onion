package de.tum.p2p.onion.forwarding.netty;

import de.tum.p2p.Peer;
import de.tum.p2p.onion.forwarding.*;
import de.tum.p2p.onion.forwarding.netty.handler.DatagramLengthFieldBasedFrameDecoder;
import de.tum.p2p.onion.forwarding.netty.handler.DatagramLengthFieldPrepender;
import de.tum.p2p.onion.forwarding.netty.handler.MissizedDatagramDiscarder;
import de.tum.p2p.proto.message.Message;
import de.tum.p2p.proto.message.onion.forwarding.DatumOnionMessage;
import de.tum.p2p.proto.message.onion.forwarding.OnionMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NettyOnionForwarder implements OnionForwarder {

    private static final Logger log = LoggerFactory.getLogger(NettyOnionForwarder.class);

    static {
        // Enable Netty to use Sl4j
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    // Length-prefix framing
    private static final int FRAME_LENGTH_PREFIX_LENGTH = Message.LENGTH_PREFIX_BYTES;
    private static final int FRAME_MAX_LENGTH = 64 * 1024;

    private final Channel channel;

    private NettyOnionForwarder(NettyOnionForwarderBuilder builder) {
        this.channel = buildServerChannel(builder.inetAddress, builder.port, builder.channel,
            builder.eventLoopExecutors, builder.channelOptions, builder.loggerLevel);
    }

    @Override
    public Tunnel createTunnel(Peer destination, int hops) throws OnionTunnelingException {
        throw new NotImplementedException();
    }

    @Override
    public void destroyTunnel(Tunnel tunnel) throws OnionTunnelingException {
        throw new NotImplementedException();
    }

    @Override
    public void forward(Tunnel tunnel, Message message) throws OnionDataForwardingException {
        throw new NotImplementedException();
    }

    @Override
    public void cover(int size) throws OnionCoverInterferenceException {
        throw new NotImplementedException();
    }

    @Override
    public void onDatumArrival(Consumer<DatumOnionMessage> datumOnionMessageConsumer) {
        throw new NotImplementedException();
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.disconnect();
            this.channel.close().syncUninterruptibly();
        } catch (Exception e) {
            log.error("Failed to close onion server channel", e);
            throw new IOException("Failed to close onion server channel", e);
        }
    }

    private Channel buildServerChannel(InetAddress inetAddress, Integer port,
                                       Class<? extends DatagramChannel> channel, EventLoopGroup eventExecutors,
                                       Map<ChannelOption, Object> opts, LogLevel logLevel) {
        log.debug("Initializing server channel on {}:{}, channel {}, opts {}, logger level {}",
            inetAddress.getHostAddress(), port, channel.getName(), opts.toString(), logLevel);

        val b = new Bootstrap();

        b.group(eventExecutors)
            .channel(channel);
        opts.forEach(b::option);

        b.handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(DatagramChannel ch) throws Exception {
                val pipe = ch.pipeline();

                if (logLevel != null)
                    pipe.addLast(new LoggingHandler(logLevel));

                // ->O Input decoding & processing
                pipe.addLast(new DatagramLengthFieldBasedFrameDecoder(FRAME_MAX_LENGTH, 0,
                    FRAME_LENGTH_PREFIX_LENGTH, -FRAME_LENGTH_PREFIX_LENGTH, FRAME_LENGTH_PREFIX_LENGTH, true));
                pipe.addLast(new MissizedDatagramDiscarder(OnionMessage.BYTES));

                pipe.addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
                        System.out.println("New data from " + msg.sender().toString()
                            + " has arrived: "
                            + msg.content().toString(Charset.defaultCharset()));
                    }
                });

                // <-O Output encoding
                pipe.addLast(new DatagramLengthFieldPrepender(FRAME_LENGTH_PREFIX_LENGTH, true));
            }
        });

        try {
            val chl = b.bind(inetAddress, port).syncUninterruptibly().channel();

            log.info("Server channel set up on {}:{}", inetAddress.getHostAddress(), port);
            return chl;
        } catch (Exception e) {
            throw new OnionInitializationException("Failed to init onion server", e);
        }
    }

    private ChannelFuture send(InetSocketAddress destination, ByteBuf data) {
        val datagramPacket = new DatagramPacket(data, destination);

        return channel.writeAndFlush(datagramPacket);
    }

    public static final class NettyOnionForwarderBuilder {

        private static final Integer DEFAULT_PORT = 1080; // SOCKS (> 1024 no root)

        private EventLoopGroup eventLoopExecutors;

        private Class<? extends DatagramChannel> channel;

        private Map<ChannelOption, Object> channelOptions;

        private InetAddress inetAddress;

        private Integer port;

        private LogLevel loggerLevel;

        public NettyOnionForwarderBuilder() {
            this.eventLoopExecutors = new NioEventLoopGroup();

            this.channel = NioDatagramChannel.class;
            this.channelOptions = new HashMap<ChannelOption, Object>() {{
                put(ChannelOption.SO_BROADCAST, true);
            }};

            try {
                this.inetAddress = InetAddress.getByName("localhost");
            } catch (UnknownHostException e) {
                // ignore (since it's just a default value
            }
            this.port = DEFAULT_PORT;
        }

        public NettyOnionForwarderBuilder eventLoopExecutors(EventLoopGroup serverExecutors) {
            this.eventLoopExecutors = serverExecutors;
            return this;
        }

        public NettyOnionForwarderBuilder channel(Class<? extends DatagramChannel> channel) {
            this.channel = channel;
            return this;
        }

        public NettyOnionForwarderBuilder channelOptions(ChannelOption option, Object value) {
            this.channelOptions.put(option, value);
            return this;
        }

        public NettyOnionForwarderBuilder inetAddress(InetAddress inetAddress) {
            this.inetAddress = inetAddress;
            return this;
        }

        public NettyOnionForwarderBuilder port(Integer port) {
            this.port = port;
            return this;
        }

        public NettyOnionForwarderBuilder loggerLevel(LogLevel loggerLevel) {
            this.loggerLevel = loggerLevel;
            return this;
        }

        public NettyOnionForwarder build() {
            return new NettyOnionForwarder(this);
        }
    }
}
