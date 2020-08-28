package io.marlinski.libdetect;

import java.net.InetAddress;
import java.net.Socket;

public class PeerReachable {
    public InetAddress address;
    public Socket socket;

    PeerReachable(InetAddress address, Socket socket) {
        this.address = address;
        this.socket = socket;
    }
}
