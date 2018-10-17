package io.left.rightmesh.libdetect;

public interface ActionListener {
    void onPeerReachable(PeerReachable peer);
    void onPeerUnreachable(PeerUnreachable peer);
}
