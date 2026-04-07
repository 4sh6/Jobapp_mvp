package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendPasswordReset(String to, String resetLink) {
        if (mailSender == null) {
            log.info("Password reset link for {}: {} (mailSender not configured)", to, resetLink);
            System.out.println("Password reset link for " + to + ": " + resetLink);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Reset your JobApp password");
        msg.setText("Click the link below to reset your password. It expires in 1 hour.\n\n" + resetLink
                + "\n\nIf you did not request this, ignore this email.");
        try {
            mailSender.send(msg);
            log.info("Sent password reset email to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}: {}", to, ex.getMessage());
            System.out.println("Password reset link for " + to + ": " + resetLink);
        }
    }

    public void sendInviteNotification(String to, String candidateName,
                                        String jobTitle, String companyName,
                                        String dashboardUrl) {
        if (mailSender == null) {
            log.info("Invite notification for {}: {} at {} (mailSender not configured)", to, jobTitle, companyName);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("You have a new interview invitation — " + jobTitle + " at " + companyName);
        msg.setText("Hi " + candidateName + ",\n\n"
                + companyName + " has invited you to interview for the role of " + jobTitle + ".\n\n"
                + "Log in to your dashboard to review and respond:\n" + dashboardUrl + "\n\n"
                + "— The HireFlow Team");
        try {
            mailSender.send(msg);
            log.info("Sent invite notification to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send invite notification to {}: {}", to, ex.getMessage());
        }
    }

    public void sendOtp(String to, String otp) {
        if (mailSender == null) {
            // In dev, just log to console
            log.info("OTP for {} is {} (mailSender not configured)", to, otp);
            System.out.println("OTP for " + to + " is " + otp);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your Verification OTP");
        msg.setText("Your OTP is: " + otp + ". It is valid for 10 minutes.");
        try {
            mailSender.send(msg);
            log.info("Sent OTP email to {}", to);
        } catch (MailException ex) {
            // Log the failure, but do NOT rethrow — OTP should still be usable for verification
            log.error("Failed to send OTP email to {}: {}", to, ex.getMessage());
            System.err.println("Failed to send OTP email to " + to + ": " + ex.getMessage());
            // Optionally still print the OTP to console so developers can proceed in dev environment
            log.info("Falling back — OTP for {} is {}", to, otp);
            System.out.println("OTP for " + to + " is " + otp);
        }
    }
}
