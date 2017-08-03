package de.tum.p2p.proto.message.onion.forwarding;

/**
 * A class implements the {@code Peelable} interface if it's eligible
 * for layered decryption of its payload.
 *
 * @param <T> a peelable Class
 *
 * @author Illia Ovchynnikov <illia.ovchynnikov@gmail.com>
 */
public interface Peelable<T extends TunnelMessage> {

    /**
     * Returns copy of itself with replaced peeled payload
     *
     * @param peeledPayload new payload (potentially decrypted)
     * @return copy with replaced payload
     */
    T peel(byte[] peeledPayload);
}
