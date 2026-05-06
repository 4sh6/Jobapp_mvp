package com.example.controller.jobseeker;

import com.example.dto.JobseekerRegistrationDto;
import com.example.model.Jobseeker;
import com.example.repository.JobseekerRepository;
import com.example.service.JobseekerService;
import com.example.service.OtpService;
import com.example.service.PasswordResetService;
import com.example.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/jobseeker")
public class JobseekerAuthController {

    @Autowired private JobseekerService jobseekerService;
    @Autowired private JobseekerRepository jobseekerRepository;
    @Autowired private OtpService otpService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private RateLimitService rateLimitService;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(required = false) String email,
                                   @RequestParam(required = false) String ref,
                                   @RequestParam(required = false) String error,
                                   Model model,
                                   HttpServletRequest request) {
        request.getSession(); // force session before large CSS block commits the response
        JobseekerRegistrationDto dto = new JobseekerRegistrationDto();
        if (email != null) dto.setEmail(email);
        if (ref != null) dto.setReferredByCode(ref.trim().toUpperCase());
        model.addAttribute("jobseekerRegistrationDto", dto);
        if (error != null) model.addAttribute("error", error);
        if (ref != null && !ref.isBlank()) model.addAttribute("referredByCode", ref.trim().toUpperCase());
        return "jobseeker/register";
    }

    @PostMapping("/register")
    public String registerJobseeker(@ModelAttribute("jobseekerRegistrationDto") @Valid JobseekerRegistrationDto dto,
                                    BindingResult result,
                                    Model model) {
        if (result.hasErrors()) return "jobseeker/register";
        try {
            Jobseeker created = jobseekerService.registerJobseeker(dto);
            otpService.generateAndSendOtp(created.getEmail());
            model.addAttribute("email", created.getEmail());
            return "jobseeker/verify-otp";
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Email already in use")) {
                model.addAttribute("emailExists", true);
            } else {
                model.addAttribute("error", ex.getMessage());
            }
            return "jobseeker/register";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(@RequestParam("email") String email, Model model) {
        model.addAttribute("email", email);
        return "jobseeker/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String handleVerifyOtp(@RequestParam("email") String email,
                                  @RequestParam("code") String code,
                                  Model model,
                                  HttpSession session,
                                  HttpServletRequest request) {
        String rateLimitKey = "otp:" + email.toLowerCase();
        if (!rateLimitService.isAllowed(rateLimitKey, 5, 600)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Too many attempts. Please wait 10 minutes before trying again.");
            return "jobseeker/verify-otp";
        }

        boolean ok = otpService.verifyOtp(email, code);
        if (!ok) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Invalid or expired OTP. Please request a new one.");
            return "jobseeker/verify-otp";
        }
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email).orElseThrow();
        jobseeker.setEmailVerified(true);
        jobseeker.setVerifiedAt(LocalDateTime.now());
        jobseekerRepository.save(jobseeker);

        // Auto-login so the user is authenticated for the rest of onboarding
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_JOBSEEKER"))));
        SecurityContextHolder.setContext(ctx);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);

        session.removeAttribute("pending_onboarding_email");
        return "redirect:/jobseeker/onboarding";
    }

    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request) {
        // Force session creation before Thymeleaf renders, so the JSESSIONID cookie is
        // written into the response headers before the CSS block fills Tomcat's 8KB buffer
        // and commits the response — otherwise CSRF token generation and session.* access fail.
        request.getSession();
        return "jobseeker/login";
    }

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "jobseeker/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String email,
                                       HttpServletRequest request,
                                       Model model) {
        String ip = request.getRemoteAddr();
        if (!rateLimitService.isAllowed("forgot-pwd:" + ip, 3, 3600)) {
            model.addAttribute("error", "Too many requests. Please try again later.");
            return "jobseeker/forgot-password";
        }

        String baseUrl = request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() != 80 && request.getServerPort() != 443
                   ? ":" + request.getServerPort() : "");
        passwordResetService.initiateReset(email, baseUrl);
        model.addAttribute("message", "If that email is registered, you will receive a reset link shortly.");
        return "jobseeker/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(@RequestParam String token, Model model) {
        if (!passwordResetService.isValidToken(token)) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
            return "jobseeker/forgot-password";
        }
        model.addAttribute("token", token);
        return "jobseeker/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match.");
            return "jobseeker/reset-password";
        }
        if (password.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Password must be at least 8 characters.");
            return "jobseeker/reset-password";
        }
        boolean success = passwordResetService.resetPassword(token, password);
        if (!success) {
            model.addAttribute("error", "This reset link is invalid or has expired. Please request a new one.");
            return "jobseeker/forgot-password";
        }
        return "redirect:/jobseeker/login?passwordReset=true";
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, Model model) {
        jobseekerRepository.findByEmail(email.trim().toLowerCase()).ifPresent(js -> {
            if (!js.isEmailVerified()) {
                otpService.resendOtp(email.trim().toLowerCase());
            }
        });
        model.addAttribute("email", email);
        model.addAttribute("info", "A new OTP has been sent to your email.");
        return "jobseeker/verify-otp";
    }
}
