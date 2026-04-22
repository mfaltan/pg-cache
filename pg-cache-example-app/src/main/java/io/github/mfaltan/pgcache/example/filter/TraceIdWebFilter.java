package io.github.mfaltan.pgcache.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdWebFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        //for debugging purposes
        var traceId = UUID.randomUUID().toString();

        try {
            MDC.put(TRACE_ID, traceId);
            response.setHeader(HEADER, traceId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }
}