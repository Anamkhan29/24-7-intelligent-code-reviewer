package com.aim.reviewer.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight fixed-window rate limiter for /api routes, keyed by client IP.
 * In-memory and per-instance — fine for the audition build. For production scale-out,
 * back this with a shared store (e.g. Redis) or an API gateway.
 */
@Component
@Order(1) // runs before auth so unauthenticated floods are throttled too
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT_PER_MINUTE = 60;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        long start;
        int count;
        Window(long start, int count) { this.start = start; this.count = count; }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/") && !allow(clientIp(request))) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Too many requests — slow down.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized boolean allow(String ip) {
        long now = System.currentTimeMillis();
        Window w = windows.get(ip);
        if (w == null || now - w.start > WINDOW_MS) {
            windows.put(ip, new Window(now, 1));
            return true;
        }
        if (w.count >= LIMIT_PER_MINUTE) {
            return false;
        }
        w.count++;
        return true;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim(); // first hop = original client on Cloud Run
        }
        return request.getRemoteAddr();
    }
}
