package io.marlinski.libdetect;

public interface ActionListener {
    void onPeerReachable(PeerReachable peer);
    void onPeerUnreachable(PeerUnreachable peer);
}
