package com.example.security;

import com.example.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RecruiterLoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 600; // 10 minutes

    @Autowired
    private RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/recruiter/login".equals(request.getServletPath())) {
            String ip = request.getRemoteAddr();
            if (!rateLimitService.isAllowed("recruiter-login:" + ip, MAX_ATTEMPTS, WINDOW_SECONDS)) {
                response.sendRedirect("/recruiter/login?rateLimited");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}