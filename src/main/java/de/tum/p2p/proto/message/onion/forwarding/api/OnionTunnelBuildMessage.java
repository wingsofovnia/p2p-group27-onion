package de.tum.p2p.proto.message.onion.forwarding.api;

import de.tum.p2p.Peer;
import de.tum.p2p.proto.message.MessageType;
import de.tum.p2p.util.Keys;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.val;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.PublicKey;

@ToString
@EqualsAndHashCode(callSuper = true)
public class OnionTunnelBuildMessage extends OnionApiMessage {

    private Peer destinationPeer;

    public OnionTunnelBuildMessage(Peer destinationPeer) {
        super(MessageType.ONION_TUNNEL_BUILD);
        this.destinationPeer = destinationPeer;
    }

    public static OnionTunnelBuildMessage me(Peer destinationPeer) { return new OnionTunnelBuildMessage(destinationPeer); }

    public Peer getDestinationPeer() {
        return destinationPeer;
    }

    @Override
    protected ByteBuffer writeMessage(ByteBuffer typedMessageBuffer) {
        throw new UnsupportedOperationException();
    }

    public static OnionTunnelBuildMessage fromBytes(ByteBuffer buffer) throws Exception {
        int minSize = 28;
        if (buffer.remaining() < minSize) {
            throw new Exception("Exceeds size of message");
        } else {

            buffer.position(MessageType.BYTES + Short.BYTES - 1);
            val ipver = buffer.get();
            int port = Short.toUnsignedInt(buffer.getShort());

            PublicKey targetHostkey;
            byte[] addressBytes;

            addressBytes = ipver == 0 ? new byte[4] : new byte[16];

            try {
                addressBytes = buffer.get(addressBytes).array();
                targetHostkey = Keys.parsePublicKey(addressBytes);
            } catch (Exception e) {
                throw new Exception("Invalid IP address");
            }

            InetSocketAddress targetAddress;
            try {
                targetAddress = new InetSocketAddress(InetAddress.getByAddress(addressBytes), port);
            } catch (UnknownHostException e) {
                throw new Exception("Invalid IP address");
            }

            Peer destinationPeer = new Peer(targetAddress.getAddress(), targetAddress.getPort(), targetHostkey);
            OnionTunnelBuildMessage message = new OnionTunnelBuildMessage(destinationPeer);
            return message;
        }
    }
}
