package com.example.config;

import com.example.service.JobseekerUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JobseekerUserDetailsService jobseekerUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider jobseekerAuthProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(jobseekerUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain jobseekerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/jobseeker/**")
                .authenticationProvider(jobseekerAuthProvider())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/jobseeker/register",
                                "/jobseeker/login",
                                "/jobseeker/send-otp",
                                "/jobseeker/verify-otp",
                                "/jobseeker/profile-details",
                                "/jobseeker/resume-upload"
                        ).permitAll()
                        .requestMatchers("/jobseeker/**").authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/jobseeker/login")
                        .loginProcessingUrl("/jobseeker/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/jobseeker/dashboard")
                        .failureUrl("/jobseeker/login?error=true")  // Use this instead of custom handler
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/jobseeker/logout")
                        .logoutSuccessUrl("/jobseeker/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/jobseeker/send-otp",
                                "/jobseeker/verify-otp"
                        )
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain recruiterFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/recruiter/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/recruiter/register", "/recruiter/login").permitAll()
                        .requestMatchers("/recruiter/**").authenticated()
                           )
                .formLogin(form -> form
                        .loginPage("/recruiter/login")
                        .loginProcessingUrl("/recruiter/login")
                        .defaultSuccessUrl("/recruiter/dashboard")
                        .failureUrl("/recruiter/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/recruiter/logout")
                        .logoutSuccessUrl("/recruiter/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/error").permitAll()
                        .requestMatchers("/actuator/**", "/admin/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}