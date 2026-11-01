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

/** Plan requests are tiny, so anything large or of unknown size is refused before it's read. */
public class RequestSizeFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_BYTES = 4_096;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long length = request.getContentLengthLong();
        if (length < 0) {
            reject(response, HttpStatus.LENGTH_REQUIRED, "Send a Content-Length header with this request.");
        } else if (length > MAX_BODY_BYTES) {
            reject(response, HttpStatus.CONTENT_TOO_LARGE, "This request is too large.");
        } else {
            chain.doFilter(request, response);
        }
    }

    private void reject(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"status\":%d,\"detail\":\"%s\"}".formatted(status.value(), detail));
    }
}
