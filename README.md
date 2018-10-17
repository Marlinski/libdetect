# libdetect
[![](https://jitpack.io/v/RightMesh/libdetect.svg)](https://jitpack.io/#RightMesh/libdetect)

performs discovery of peer and service on the LAN using brute force TCP knocking on all 
connected subnets.

You can use this library in your project with gradle using jitpack:

```java
repositories {
    maven { url 'https://jitpack.io' }
}
```

```java
dependencies {
   implementation 'com.github.compscidr:libdetect:master-SNAPSHOT'
}
```

# Features

* Single threaded using librxtcp and rxjava
* Supports callbacks for the following two events:
  * PeerReachable events generated when a peer becomes reachable on the local network on the specified TCP port.
  * PeerUnreachable events generated when a previously reachable peer is no longer reachable on the specified TCP port

## Register for TCP peer discovery:

The port used by LibDetect is used for discovery only and is not expected to be used for an actual communication
by the calling service. It only be meant to use to identify the service of the caller. You can start
a discovery by calling LibDetect.start(port, callback) like so:

```

LibDetectHandle handle = LibDetect.start(4000, new ActionListener() {
    @Override
    public void onPeerReachable(PeerReachable peer) {
        System.out.println("PEER REACHABLE on " + peer.address.getHostAddress());
    }

    @Override
    public void onPeerUnreachable(PeerUnreachable peer) {
        System.out.println("PEER UNREACHABLE on " + peer.address.getHostAddress());
    }
}, false);
```

to stop LibDetect from monitoring the LAN, use the stop() method on the handle:

```
handle.stop();
```
