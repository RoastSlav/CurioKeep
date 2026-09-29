package org.rostislav.curiokeep.items;

import java.net.InetAddress;

/** Decides whether the server may open a connection to an address on behalf of a user-supplied URL. */
final class OutboundAddressPolicy {

    private OutboundAddressPolicy() {
    }

    /** True only for globally routable unicast addresses: loopback, private, link-local and similar ranges are refused. */
    static boolean isPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) {
            return false;
        }
        byte[] b = address.getAddress();
        if (b.length == 4) {
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            if (first == 0 || first >= 240) return false;
            if (first == 100 && second >= 64 && second <= 127) return false; // carrier-grade NAT
            if (first == 198 && (second == 18 || second == 19)) return false; // benchmarking
            return !(first == 192 && second == 0 && (b[2] & 0xFF) == 0); // IETF protocol assignments
        }
        return (b[0] & 0xFE) != 0xFC; // unique local addresses, fc00::/7
    }
}
