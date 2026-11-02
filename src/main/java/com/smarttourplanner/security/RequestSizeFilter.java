package com.smarttourplanner.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API requests are small, so anything large or of unknown size is refused before it's read. Admin
 * saves get more room, because notes can be long and Bangla text takes three bytes a letter.
 */
public class RequestSizeFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_BYTES = 4_096;
    private static final int MAX_ADMIN_BODY_BYTES = 32_768;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        boolean sendsBody = HttpMethod.POST.matches(request.getMethod()) || HttpMethod.PUT.matches(request.getMethod());
        return !sendsBody || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length < 0) {
            reject(response, HttpStatus.LENGTH_REQUIRED, "Send a Content-Length header with this request.");
        } else if (length > limitFor(request.getRequestURI())) {
            reject(response, HttpStatus.CONTENT_TOO_LARGE, "This request is too large.");
        } else {
            chain.doFilter(request, response);
        }
    }

    private static int limitFor(String path) {
        return path.startsWith("/api/v1/admin/") ? MAX_ADMIN_BODY_BYTES : MAX_BODY_BYTES;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"detail\":\"%s\"}".formatted(status.value(), detail));
    }
}
