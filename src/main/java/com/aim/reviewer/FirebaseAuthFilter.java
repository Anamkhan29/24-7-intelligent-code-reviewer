package com.aim.reviewer.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Verifies the Firebase ID token on every protected /api route and exposes the uid as a
 * request attribute. Deny-by-default: no valid token, no access. The token is never logged.
 * /api/health and static assets are public.
 */
@Component
@Order(2) // runs after rate limiting
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final Logger log = Logger.getLogger(FirebaseAuthFilter.class.getName());

    private static boolean isProtected(String path) {
        return path.startsWith("/api/") && !path.equals("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (isProtected(request.getRequestURI())) {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                deny(response);
                return;
            }
            String token = header.substring(7).trim();
            try {
                FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(token);
                request.setAttribute("uid", decoded.getUid());
            } catch (Exception e) {
                log.warning("ID token verification failed" + e.getMessage());
                deny(response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void deny(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"detail\":\"Invalid or expired token\"}");
    }
}
