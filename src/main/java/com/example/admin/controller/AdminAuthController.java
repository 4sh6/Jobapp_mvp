package com.example.admin.controller;

import com.example.admin.dto.AdminLoginRequest;
import com.example.security.JwtUtil;
import com.example.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    @Autowired
    private InMemoryUserDetailsManager adminUserDetailsManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitService rateLimitService;

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 600; // 10 minutes

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AdminLoginRequest req,
                                   HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        if (!rateLimitService.isAllowed("admin-login:" + ip, MAX_ATTEMPTS, WINDOW_SECONDS)) {
            return ResponseEntity.status(429).body(
                Map.of("error", "Too many login attempts. Please wait 10 minutes before trying again."));
        }

        UserDetails user;
        try {
            user = adminUserDetailsManager.loadUserByUsername(req.getUsername());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtUtil.generateToken(req.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", req.getUsername()));
    }
}