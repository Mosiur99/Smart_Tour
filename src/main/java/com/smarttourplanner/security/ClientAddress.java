package com.smarttourplanner.security;

import jakarta.servlet.http.HttpServletRequest;

/** Finds the visitor's address. The Cloudflare header is trusted only when the site runs behind Cloudflare. */
public class ClientAddress {

    private static final String CLOUDFLARE_HEADER = "CF-Connecting-IP";

    private final boolean behindCloudflare;

    public ClientAddress(boolean behindCloudflare) {
        this.behindCloudflare = behindCloudflare;
    }

    public String of(HttpServletRequest request) {
        String forwarded = request.getHeader(CLOUDFLARE_HEADER);
        if (behindCloudflare && forwarded != null && !forwarded.isBlank()) {
            return forwarded;
        }
        return request.getRemoteAddr();
    }
}
