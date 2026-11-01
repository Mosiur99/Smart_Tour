package com.smarttourplanner.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressTest {

    @Test
    void ignoresTheCloudflareHeaderWhenNotBehindCloudflare() {
        assertThat(new ClientAddress(false).of(requestFrom("10.0.0.1", "1.2.3.4"))).isEqualTo("10.0.0.1");
    }

    @Test
    void usesTheCloudflareHeaderBehindCloudflare() {
        assertThat(new ClientAddress(true).of(requestFrom("10.0.0.1", "1.2.3.4"))).isEqualTo("1.2.3.4");
    }

    @Test
    void fallsBackToTheConnectionAddress() {
        assertThat(new ClientAddress(true).of(requestFrom("10.0.0.1", null))).isEqualTo("10.0.0.1");
    }

    private static MockHttpServletRequest requestFrom(String address, String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(address);
        if (header != null) {
            request.addHeader("CF-Connecting-IP", header);
        }
        return request;
    }
}
