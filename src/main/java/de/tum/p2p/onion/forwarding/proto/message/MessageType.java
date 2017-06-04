package de.tum.p2p.onion.forwarding.proto.message;

import lombok.val;

public enum MessageType {
    UNKNOWN(0),
    ONION_TUNNEL_BUILD(560),
    ONION_TUNNEL_READY(561),
    ONION_TUNNEL_INCOMING(562),
    ONION_TUNNEL_DESTROY(563),
    ONION_TUNNEL_DATA(564),
    ONION_ERROR(565),
    ONION_COVER(566);

    public static final int BYTES = 2;

    private final short code;

    MessageType(Integer code) {
        this.code = code.shortValue();
    }

    public short code() {
        return this.code;
    }

    public static MessageType fromCode(short code) {
        for (val msgType : MessageType.values())
            if (msgType.code() == code)
                return msgType;

        return UNKNOWN;
    }
}
