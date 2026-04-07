package com.example.config;

import com.example.security.JwtAuthFilter;
import com.example.service.JobseekerUserDetailsService;
import com.example.service.recruiter.RecruiterUserDetailsService;
import com.example.repositary.JobseekerRepository;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.ServletException;
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
            response.sendRedirect("/jobseeker/dashboard");
        };
    }

    // ===========================
    // JOBSEEKER FAILURE HANDLER
    // ===========================
    @Bean
    public AuthenticationFailureHandler jobseekerFailureHandler() {
        return (request, response, exception) -> {
            String email = request.getParameter("username");
            if (exception.getCause() instanceof UsernameNotFoundException ||
                    (exception.getMessage() != null && exception.getMessage().contains("Jobseeker not found"))) {
                response.sendRedirect("/jobseeker/register?email=" + email
                        + "&error=Email+not+registered.+Please+sign+up.");
            } else {
                response.sendRedirect("/jobseeker/login?error=Invalid+email+or+password");
            }
        };
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
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
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
                .logout(logout -> logout.logoutSuccessUrl("/jobseeker/login?logout"));

        return http.build();
    }

    // ─── Recruiter Security ───
    @Bean
    @Order(4)
    public SecurityFilterChain recruiterSecurity(HttpSecurity http) throws Exception {
        http.userDetailsService(recruiterUserDetailsService);
        http
                .securityMatcher("/recruiter/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/recruiter/register", "/recruiter/login").permitAll()
                        .anyRequest().hasRole("RECRUITER")
                )
                .formLogin(form -> form
                        .loginPage("/recruiter/login")
                        .defaultSuccessUrl("/recruiter/dashboard", true)
                )
                .logout(logout -> logout.logoutSuccessUrl("/recruiter/login?logout"));

        return http.build();
    }
}
