package io.marlinski.libdetect;

import java.net.InetAddress;

public class PeerUnreachable {
    public InetAddress address;

    PeerUnreachable(InetAddress address) {
        this.address = address;
    }
}