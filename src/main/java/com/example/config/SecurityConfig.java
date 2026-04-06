package com.example.config;

import com.example.service.JobseekerUserDetailsService;
import com.example.service.recruiter.RecruiterUserDetailsService;
import com.example.repositary.JobseekerRepository;
import com.example.model.Jobseeker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JobseekerUserDetailsService jobseekerUserDetailsService;

    @Autowired
    private RecruiterUserDetailsService recruiterUserDetailsService;

    @Autowired
    private JobseekerOAuth2UserService jobseekerOAuth2UserService;

    @Autowired
    private JobseekerRepository jobseekerRepository;

    // ===========================
    // JOBSEEKER SUCCESS HANDLER
    // ===========================
    @Bean
    public AuthenticationSuccessHandler jobseekerSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                              HttpServletResponse response,
                                              Authentication authentication) throws IOException, ServletException {
                String email = authentication.getName();

                // Check if jobseeker exists and profile is completed
                Optional<Jobseeker> jobseeker = jobseekerRepository.findByEmail(email.trim().toLowerCase());

                if (jobseeker.isPresent()) {
                    Jobseeker js = jobseeker.get();

                    // If profile not completed, redirect to onboarding
                    if (!js.isProfileCompleted()) {
                        response.sendRedirect("/jobseeker/onboarding?email=" + email);
                        return;
                    }

                    // If resume not uploaded, redirect to resume upload
                    if (!js.isResumeUploaded()) {
                        response.sendRedirect("/jobseeker/resume-onboarding?email=" + email);
                        return;
                    }
                }

                // All complete, go to dashboard
                response.sendRedirect("/jobseeker/dashboard");
            }
        };
    }

    // ===========================
    // JOBSEEKER FAILURE HANDLER
    // ===========================
    @Bean
    public AuthenticationFailureHandler jobseekerFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                                                HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
                
                String email = request.getParameter("username");
                
                // Check if the failure is because user doesn't exist
                if (exception.getCause() instanceof UsernameNotFoundException || 
                    exception.getMessage().contains("Jobseeker not found")) {
                    
                    // Redirect to registration with prefilled email
                    response.sendRedirect("/jobseeker/register?email=" + email + "&error=Email not registered. Please sign up.");
                } else {
                    // Default error (wrong password, etc.)
                    response.sendRedirect("/jobseeker/login?error=Invalid email or password");
                }
            }
        };
    }


    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurity(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/admin/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .anyRequest().hasRole("ADMIN")
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
                )
                .logout(logout -> logout.logoutSuccessUrl("/admin/login?logout"));

        return http.build();
    }

    // ===========================
    // 2️⃣ JOBSEEKER SECURITY
    // ===========================
    @Bean
    @Order(2)
    public SecurityFilterChain jobseekerSecurity(HttpSecurity http) throws Exception {

        // Register the Jobseeker-specific UserDetailsService for this security chain
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
                                userInfo.userService(jobseekerOAuth2UserService)
                        )
                        .successHandler(jobseekerSuccessHandler())
                        .failureUrl("/jobseeker/login?error")
                )
                .logout(logout -> logout.logoutSuccessUrl("/jobseeker/login?logout"));

        return http.build();
    }

    // ===========================
    // 3️⃣ RECRUITER SECURITY
    // ===========================
    @Bean
    @Order(3)
    public SecurityFilterChain recruiterSecurity(HttpSecurity http) throws Exception {

        // Register the Recruiter-specific UserDetailsService for this security chain
        http.userDetailsService(recruiterUserDetailsService);

        http
                .securityMatcher("/recruiter/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/recruiter/register",
                                "/recruiter/login"
                        ).permitAll()
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