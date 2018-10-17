package io.left.rightmesh.libdetect;

import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;

import io.left.rightmesh.librxtcp.RxTCP;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/*
 * Detects other devices on the same subnet also using io.left.rightmesh.libdetect.LibDetect on the same port.
 * Provides events for devices being reachable and unreachable
 * Jason Ernst, 2018, RightMesh
 *
 * This lib works by brute forcing a TCP connection on the specified port for all IPs on all subnet ranges for which
 * a device has a local ipv4 address on. For instance if the device has local ip 192.168.1.45, it will search every ip
 * from 192.168.1.1 to 192.168.1.254 excluding itself (if skipOurself is set). If a connection is made it calls the
 * PeerReachaable callback. If this TCP connection subsequently breaks, it will call the io.left.rightmesh.libdetect.PeerUnreachable callback.
 * In order to detect the breakage a 1 byte null byte is sent periodically after the connection is made - java does not
 * throw an exception on a socket when the remote side of the socket closes for instance until a read or write fails.
 *
 * Note: it takes some time to cleanup all of the threads so on the stop() call. This increases with the number of
 * ports being monitored, and the number of interfaces with local ipv4 address present on the device.
 *
 */
public class LibDetect {

    public interface LibDetectHandle {
        void stop();
    }

    public static LibDetectHandle start(int port, ActionListener actionListener, boolean skipOurself) {
        RxTCP.Server<RxTCP.Connection> s = new RxTCP.Server<>(port);
        s.start().subscribe(
                connection -> {
                    handleEvent(connection, actionListener);
                },
                e -> {
                    /* the server has stopped */
                },
                () -> {
                    /* the server has stopped */
                }
        );

        getInetAddresses()
                .flatMap(inetAddress -> getSubnetIPAddresses(inetAddress, skipOurself))
                .subscribe(
                        inetAddress -> {
                            new RxTCP.ConnectionRequest<>(inetAddress.getHostAddress(), port)
                                    .connect()
                                    .subscribe(
                                            connection -> {
                                                handleEvent(connection, actionListener);
                                            },
                                            e -> {
                                                /* ignore */
                                            });
                        });
        return s::stop;
    }

    public static void handleEvent(RxTCP.Connection con, ActionListener actionListener) {
        try {
            final InetAddress inet = con.channel.socket().getInetAddress();
            actionListener.onPeerReachable(
                    new PeerReachable(
                            con.channel.socket().getInetAddress(),
                            con.channel.socket())
            );
            con.send(Flowable.just(ByteBuffer.wrap("HELLO".getBytes())));
            con.recv().subscribe(
                    byteBuffer -> {
                        /* ignore */
                        byteBuffer.position(byteBuffer.limit());
                    },
                    e -> {
                        actionListener.onPeerUnreachable(
                                new PeerUnreachable(inet));
                    },
                    () -> {
                        actionListener.onPeerUnreachable(
                                new PeerUnreachable(inet));
                    });
        } catch (Exception e) {
            /* ignore */
        }
    }


    /**
     * Will return a set of subnet IP addresses given the ip addresse provided. Will exclude the given ip address from
     * the set.
     *
     * @param ip the ip to determine the other subnet ips for
     * @return the set of ip addresses in the same subnet.
     */
    private static Observable<InetAddress> getSubnetIPAddresses(InetAddress ip, boolean skipOurself) {
        if (ip instanceof Inet4Address) {
            final String root = ip.getHostAddress();
            if (root.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")) {
                return Observable.create(s -> {
                    String subroot = root.substring(0, root.lastIndexOf('.') + 1);
                    for (int i = 1; i < 254; i++) {
                        String ipstring = subroot + i;
                        if ((ipstring.equals(root) && skipOurself) || ipstring.startsWith("127")) {
                            continue;
                        }
                        InetAddress inet;
                        try {
                            inet = Inet4Address.getByName(ipstring);
                            s.onNext(inet);
                        } catch (UnknownHostException ex) {
                            /* ignore */
                        }
                    }
                    s.onComplete();
                });
            } else {
                return Observable.empty();
            }
        } else {
            return Observable.empty();
        }
    }

    /**
     * Will return all of the local ip addresses for all of the interfaces on this device (eth, wifi, local, etc)
     *
     * @return a set of inetaddresses (ipv4 and ipv6)
     */
    private static Observable<InetAddress> getInetAddresses() {
        return Observable.create(s -> {
            try {
                Enumeration e = NetworkInterface.getNetworkInterfaces();
                while (e.hasMoreElements()) {
                    NetworkInterface n = (NetworkInterface) e.nextElement();
                    Enumeration ee = n.getInetAddresses();
                    while (ee.hasMoreElements()) {
                        s.onNext((InetAddress) ee.nextElement());
                    }
                }
            } catch (SocketException ex) {
                s.onError(ex);
            }
            s.onComplete();
        });
    }
}
