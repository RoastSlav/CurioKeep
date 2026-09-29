package org.rostislav.curiokeep.items;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;

class OutboundAddressPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "127.8.9.10", "10.0.0.5", "172.16.4.1", "192.168.1.20", "169.254.169.254", "0.0.0.0",
            "100.64.0.1", "198.18.0.1", "224.0.0.1", "255.255.255.255", "::1", "::", "fe80::1", "fc00::1", "fd12:3456::1"
    })
    void refusesNonPublicAddresses(String literal) throws UnknownHostException {
        assertThat(OutboundAddressPolicy.isPublic(InetAddress.getByName(literal))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "93.184.216.34", "2606:4700:4700::1111", "172.32.0.1", "100.63.255.255"})
    void allowsPublicAddresses(String literal) throws UnknownHostException {
        assertThat(OutboundAddressPolicy.isPublic(InetAddress.getByName(literal))).isTrue();
    }

    @Test
    void treatsAnIpv4MappedLoopbackAsLoopback() throws UnknownHostException {
        assertThat(OutboundAddressPolicy.isPublic(InetAddress.getByName("::ffff:127.0.0.1"))).isFalse();
    }
}
