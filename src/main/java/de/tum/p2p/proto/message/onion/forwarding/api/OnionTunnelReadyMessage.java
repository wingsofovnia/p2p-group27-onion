package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.onion.forwarding.TunnelId;
import de.tum.p2p.proto.message.MessageType;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelReadyMessage extends OnionApiMessage {
    private final TunnelId tunnelId;
    private final PublicKey publicKey;

    public OnionTunnelReadyMessage(TunnelId tunnelId, PublicKey publicKey) {
        super(MessageType.ONION_TUNNEL_READY);
        this.tunnelId = tunnelId;
        this.publicKey = publicKey;
    }

    public static OnionTunnelReadyMessage fromBytes(ByteBuffer buffer) throws Exception {

        int id = buffer.getInt();
        byte[] encoding = new byte[buffer.remaining()];
        buffer.get(encoding);
        try {
            KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(buffer.get(encoding).array()));
        } catch (Exception e) {
            throw new Exception("Invalid format");
        }

        PublicKey key = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoding));
        OnionTunnelReadyMessage message = new OnionTunnelReadyMessage(TunnelId.wrap(id), key);
        return message;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        return null;
    }
}
