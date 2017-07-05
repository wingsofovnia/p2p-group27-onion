package de.tum.p2p.proto.message;

import lombok.val;

public enum MessageType {
    UNKNOWN(0),

    // RPS API / TCP
    RPS_QUERY(540),
    RPS_PEER(541),

    // ONION API / TCP
    ONION_TUNNEL_BUILD(560),
    ONION_TUNNEL_READY(561),
    ONION_TUNNEL_INCOMING(562),
    ONION_TUNNEL_DESTROY(563),
    ONION_TUNNEL_DATA(564),
    ONION_ERROR(565),
    ONION_COVER(566),

    // ONION P2P / UDP
    ONION_TUNNEL_CREATE(570),
    ONION_TUNNEL_CREATED(571),
    ONION_TUNNEL_EXTEND(575),
    ONION_TUNNEL_EXTENDED(576),
    ONION_TUNNEL_DATUM(580),
    ONION_TUNNEL_ERROR(590);

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
