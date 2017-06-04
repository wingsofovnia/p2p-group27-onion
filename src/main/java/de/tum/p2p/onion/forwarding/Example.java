package de.tum.p2p.onion.forwarding;

import de.tum.p2p.onion.forwarding.proto.Protocol;
import lombok.experimental.var;

public class Example {
    static {
        var exampleProtoMsg = Protocol.Example.newBuilder().setFoo("Fooo").build();
    }
}
