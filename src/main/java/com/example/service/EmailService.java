package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * The address all outgoing emails are sent from.
     * Reads MAIL_USERNAME env var directly (same source as spring.mail.username).
     * Must match the authenticated SMTP account for Gmail.
     */
    @Value("${MAIL_USERNAME:noreply@koderhyre.tech}")
    private String fromAddress;

    public void sendPasswordReset(String to, String resetLink) {
        if (mailSender == null) {
            log.info("Password reset link for {}: {} (mailSender not configured)", to, resetLink);
            System.out.println("Password reset link for " + to + ": " + resetLink);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Reset your KoderHyre password");
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
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("You have a new interview invitation — " + jobTitle + " at " + companyName);
        msg.setText("Hi " + candidateName + ",\n\n"
                + companyName + " has invited you to interview for the role of " + jobTitle + ".\n\n"
                + "Log in to your dashboard to review and respond:\n" + dashboardUrl + "\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent invite notification to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send invite notification to {}: {}", to, ex.getMessage());
        }
    }

    public void sendStatusUpdateEmail(String to, String candidateName, String jobTitle,
                                       String companyName, String status, String dashboardUrl) {
        String friendlyStatus = switch (status) {
            case "SHORTLISTED"   -> "Shortlisted";
            case "UNDER_REVIEW"  -> "Under Review";
            case "HIRED"         -> "Hired";
            case "REJECTED"      -> "Not Selected";
            default              -> status;
        };

        if (mailSender == null) {
            log.info("Status update for {} — {} is now {} (mailSender not configured)", to, jobTitle, friendlyStatus);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Update on your application — " + jobTitle + " at " + companyName);
        msg.setText("Hi " + candidateName + ",\n\n"
                + companyName + " has updated your application status for " + jobTitle + ".\n\n"
                + "New status: " + friendlyStatus + "\n\n"
                + "View your applications:\n" + dashboardUrl + "\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent status update email to {} — status: {}", to, friendlyStatus);
        } catch (MailException ex) {
            log.error("Failed to send status update email to {}: {}", to, ex.getMessage());
        }
    }

    public void sendProfileApproved(String to, String candidateName) {
        if (mailSender == null) {
            log.info("Profile approved email for {} (mailSender not configured)", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Your profile has been approved — KoderHyre");
        msg.setText("Hi " + candidateName + ",\n\n"
                + "Great news! Our team has reviewed your profile and it has been approved.\n\n"
                + "You are now visible to top recruiters on KoderHyre. "
                + "Recruiters will reach out to you directly with interview opportunities.\n\n"
                + "Make sure your profile and resume are up to date to maximise your chances.\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent profile approval email to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send approval email to {}: {}", to, ex.getMessage());
        }
    }

    public void sendProfileRejected(String to, String candidateName, String reason) {
        if (mailSender == null) {
            log.info("Profile rejected email for {} (mailSender not configured)", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Update on your KoderHyre profile review");
        msg.setText("Hi " + candidateName + ",\n\n"
                + "Thank you for applying to join KoderHyre's curated talent pool.\n\n"
                + "After reviewing your profile, we are unable to approve it at this time.\n\n"
                + (reason != null && !reason.isBlank() ? "Reason: " + reason + "\n\n" : "")
                + "You are welcome to update your profile and reapply after 30 days.\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent profile rejection email to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send rejection email to {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Sent to CANDIDATE when they accept an interview invite.
     * Confirms acceptance and tells them the recruiter will reach out.
     */
    public void sendAcceptanceConfirmationToCandidate(String to, String candidateName,
                                                       String jobTitle, String companyName,
                                                       String requiredSkills, String dashboardUrl) {
        if (mailSender == null) {
            log.info("Acceptance confirmation for candidate {} — {} at {} (mailSender not configured)",
                    to, jobTitle, companyName);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Interview confirmed — " + jobTitle + " at " + companyName);
        msg.setText("Hi " + candidateName + ",\n\n"
                + "You've accepted the interview invitation for " + jobTitle + " at " + companyName + ".\n\n"
                + "The hiring team will be in touch with you shortly to schedule the interview.\n\n"
                + "In the meantime, prepare to discuss your experience with:\n"
                + requiredSkills + "\n\n"
                + "View all your interview requests here:\n" + dashboardUrl + "\n\n"
                + "Best of luck!\n— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent acceptance confirmation to candidate {}", to);
        } catch (MailException ex) {
            log.error("Failed to send acceptance confirmation to {}: {}", to, ex.getMessage());
        }
    }

    /**
     * Sent to RECRUITER when a candidate accepts their interview invite.
     * Introduces the candidate and provides their contact details.
     */
    public void sendAcceptanceNotificationToRecruiter(String to, String recruiterName,
                                                       String candidateName, String candidateEmail,
                                                       String jobTitle,
                                                       String experienceSummary, String expectedCtc,
                                                       String noticePeriod, String candidateProfileUrl) {
        if (mailSender == null) {
            log.info("Acceptance notification for recruiter {} — {} accepted {} (mailSender not configured)",
                    to, candidateName, jobTitle);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject(candidateName + " has accepted your interview request — " + jobTitle);
        msg.setText("Hi " + recruiterName + ",\n\n"
                + "Great news! " + candidateName + " has accepted your interview invitation for " + jobTitle + ".\n\n"
                + "--- Candidate Overview ---\n"
                + "Experience:    " + experienceSummary + "\n"
                + "Expected CTC:  " + expectedCtc + "\n"
                + "Notice Period: " + noticePeriod + "\n\n"
                + "--- Contact Details (unlocked) ---\n"
                + "Email: " + candidateEmail + "\n\n"
                + "View their full profile and resume:\n" + candidateProfileUrl + "\n\n"
                + "Reach out to schedule the interview.\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent acceptance notification to recruiter {}", to);
        } catch (MailException ex) {
            log.error("Failed to send acceptance notification to recruiter {}: {}", to, ex.getMessage());
        }
    }

    public void sendRecruiterApproved(String to, String recruiterName) {
        if (mailSender == null) {
            log.info("Recruiter approved email for {} (mailSender not configured)", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Your recruiter account is approved — KoderHyre");
        msg.setText("Hi " + recruiterName + ",\n\n"
                + "Your recruiter account on KoderHyre has been approved!\n\n"
                + "You can now log in and start posting jobs and browsing our curated talent pool.\n\n"
                + "Log in at: [your domain]/recruiter/login\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent recruiter approval email to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send recruiter approval email to {}: {}", to, ex.getMessage());
        }
    }

    public void sendRecruiterRejected(String to, String recruiterName) {
        if (mailSender == null) {
            log.info("Recruiter rejected email for {} (mailSender not configured)", to);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(to);
        msg.setSubject("Update on your KoderHyre recruiter account");
        msg.setText("Hi " + recruiterName + ",\n\n"
                + "Thank you for registering as a recruiter on KoderHyre.\n\n"
                + "After reviewing your account details, we are unable to approve your account at this time.\n\n"
                + "If you believe this is an error or would like to provide more information, "
                + "please contact us at support@koderhyre.tech.\n\n"
                + "— The KoderHyre Team");
        try {
            mailSender.send(msg);
            log.info("Sent recruiter rejection email to {}", to);
        } catch (MailException ex) {
            log.error("Failed to send recruiter rejection email to {}: {}", to, ex.getMessage());
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
        msg.setFrom(fromAddress);
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
