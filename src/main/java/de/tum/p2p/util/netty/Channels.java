package de.tum.p2p.util.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.val;

import java.util.concurrent.CompletableFuture;

public final class Channels {

    public static CompletableFuture<Channel> toCompletableFuture(ChannelFuture nettyChannelFuture) {
        val completableChannelFuture = new CompletableFuture<Channel>();

        nettyChannelFuture.addListener((ChannelFuture f) -> {
            if (f.isCancelled()) {
                completableChannelFuture.cancel(false);
            } else if (f.cause() != null) {
                completableChannelFuture.completeExceptionally(f.cause());
            } else {
                completableChannelFuture.complete(f.channel());
            }
        });

        return completableChannelFuture;
    }
}
