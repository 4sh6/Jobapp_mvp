package com.example.config;

import com.example.security.JwtAuthFilter;
import com.example.security.RecruiterLoginRateLimitFilter;
import com.example.service.JobseekerUserDetailsService;
import com.example.service.recruiter.RecruiterUserDetailsService;
import com.example.repository.JobseekerRepository;
import com.example.model.Jobseeker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JobseekerUserDetailsService jobseekerUserDetailsService;

    @Autowired
    private RecruiterUserDetailsService recruiterUserDetailsService;

    @Autowired
    private JobseekerOAuth2UserService jobseekerOAuth2UserService;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private RecruiterLoginRateLimitFilter recruiterLoginRateLimitFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    // CORS allowed origins — set ADMIN_CORS_ORIGINS env var in production
    @Value("${admin.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    // ─── CORS for React admin frontend ───
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    // ─── Admin UserDetailsService (shared by both Thymeleaf & JWT chains) ───
    @Bean
    public InMemoryUserDetailsManager adminUserDetailsManager() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    // ===========================
    // JOBSEEKER SUCCESS HANDLER
    // ===========================
    @Bean
    public AuthenticationSuccessHandler jobseekerSuccessHandler() {
        return (request, response, authentication) -> {
            String email = authentication.getName();
            Optional<Jobseeker> jobseeker = jobseekerRepository.findByEmail(email.trim().toLowerCase());

            if (jobseeker.isPresent()) {
                Jobseeker js = jobseeker.get();

                if (!js.isProfileCompleted()) {
                    // Store in session so onboarding can verify without URL param manipulation
                    request.getSession().setAttribute("pending_onboarding_email", email);
                    response.sendRedirect("/jobseeker/onboarding");
                    return;
                }
                if (!js.isResumeUploaded()) {
                    request.getSession().setAttribute("pending_onboarding_email", email);
                    response.sendRedirect("/jobseeker/resume-onboarding");
                    return;
                }
            }

            // Fully onboarded — return to the page they originally tried to reach
            // (e.g. clicked ATS Checker on the homepage while logged out)
            var savedRequest = new org.springframework.security.web.savedrequest.HttpSessionRequestCache()
                    .getRequest(request, response);
            if (savedRequest != null && "GET".equalsIgnoreCase(savedRequest.getMethod())) {
                response.sendRedirect(savedRequest.getRedirectUrl());
                return;
            }
            response.sendRedirect("/jobseeker/dashboard");
        };
    }

    // ===========================
    // JOBSEEKER FAILURE HANDLER
    // ===========================
    @Bean
    public AuthenticationFailureHandler jobseekerFailureHandler() {
        return (request, response, exception) -> {
            if (exception.getCause() instanceof UsernameNotFoundException ||
                    (exception.getMessage() != null && exception.getMessage().contains("Jobseeker not found"))) {
                // SECURITY: do not expose email in redirect URL (prevents email harvesting via logs/history)
                response.sendRedirect("/jobseeker/register?error=Email+not+registered.+Please+sign+up.");
            } else {
                response.sendRedirect("/jobseeker/login?error=Invalid+email+or+password");
            }
        };
    }

    // ─── H2 Console (dev only) ───
    @Bean
    @Order(0)
    public SecurityFilterChain h2ConsoleSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    // ─── Admin REST API (JWT, stateless) ───
    @Bean
    @Order(1)
    public SecurityFilterChain adminApiSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── Admin Thymeleaf (session-based, InMemoryUserDetailsManager) ───
    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")
                .userDetailsService(adminUserDetailsManager())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error")
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            boolean hadSession = hasSessionCookie(req);
                            res.sendRedirect(hadSession
                                    ? "/admin/login?sessionExpired"
                                    : "/admin/login");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID"))
                        .logoutSuccessUrl("/admin/login?logout"));

        return http.build();
    }

    // ─── Jobseeker Security ───
    @Bean
    @Order(3)
    public SecurityFilterChain jobseekerSecurity(HttpSecurity http) throws Exception {
        http.userDetailsService(jobseekerUserDetailsService);
        http
                .securityMatcher("/jobseeker/**", "/jobs/**", "/oauth2/**", "/login/oauth2/**")
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable()) // modern browsers use CSP instead
                        .contentTypeOptions(ct -> {})        // keep X-Content-Type-Options: nosniff
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(31536000))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/jobseeker/register",
                                "/jobseeker/verify-otp",
                                "/jobseeker/onboarding",
                                "/jobseeker/resume-onboarding",
                                "/jobseeker/login",
                                "/jobseeker/forgot-password",
                                "/jobseeker/reset-password",
                                "/jobseeker/resend-otp",
                                "/jobs",
                                "/jobs/*"
                        ).permitAll()
                        .anyRequest().hasRole("JOBSEEKER")
                )
                .formLogin(form -> form
                        .loginPage("/jobseeker/login")
                        .successHandler(jobseekerSuccessHandler())
                        .failureHandler(jobseekerFailureHandler())
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/jobseeker/login")
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(jobseekerOAuth2UserService))
                        .successHandler(jobseekerSuccessHandler())
                        .failureUrl("/jobseeker/login?error")
                )
                // No invalidSessionUrl — handled by the entry point below to distinguish
                // real session expiry (cookie present, session gone) from first-time access
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            // For ATS Checker, always redirect to login cleanly (no session expired message)
                            if (req.getRequestURI().contains("/ats-checker")) {
                                res.sendRedirect("/jobseeker/login");
                                return;
                            }
                            boolean hadSession = hasSessionCookie(req);
                            res.sendRedirect(hadSession
                                    ? "/jobseeker/login?sessionExpired"
                                    : "/jobseeker/login");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/jobseeker/logout")
                        // Clear the session cookie on logout so the entry point above
                        // doesn't mistake a post-logout visit for a session expiry
                        .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID"))
                        .logoutSuccessUrl("/jobseeker/login?logout"));

        return http.build();
    }

    /** Returns true if the request carries a JSESSIONID cookie (i.e. the user had a session). */
    private static boolean hasSessionCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return false;
        for (Cookie c : cookies) {
            if ("JSESSIONID".equals(c.getName())) return true;
        }
        return false;
    }

    // ─── Recruiter Security ───
    @Bean
    @Order(4)
    public SecurityFilterChain recruiterSecurity(HttpSecurity http) throws Exception {
        http.userDetailsService(recruiterUserDetailsService);
        http
                .securityMatcher("/recruiter/**")
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .xssProtection(xss -> xss.disable())
                        .contentTypeOptions(ct -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(31536000))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/recruiter/home", "/recruiter/register", "/recruiter/login").permitAll()
                        .anyRequest().hasRole("RECRUITER")
                )
                .formLogin(form -> form
                        .loginPage("/recruiter/login")
                        .defaultSuccessUrl("/recruiter/dashboard", true)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            boolean hadSession = hasSessionCookie(req);
                            res.sendRedirect(hadSession
                                    ? "/recruiter/login?sessionExpired"
                                    : "/recruiter/login");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/recruiter/logout")
                        .addLogoutHandler(new CookieClearingLogoutHandler("JSESSIONID"))
                        .logoutSuccessUrl("/recruiter/login?logout"))
                .addFilterBefore(recruiterLoginRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
