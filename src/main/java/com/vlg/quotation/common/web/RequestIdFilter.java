package com.vlg.quotation.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a request id to every HTTP request for log correlation and error
 * responses. Honors an incoming {@code X-Request-Id} header when present.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incomingRequestId = request.getHeader(REQUEST_ID_HEADER);
        String requestId = StringUtils.hasText(incomingRequestId)
                ? incomingRequestId
                : "req-" + UUID.randomUUID();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            request.removeAttribute(REQUEST_ID_ATTRIBUTE);
        }
    }
}
