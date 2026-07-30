package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email via the Resend HTTP API instead of SMTP.
 * Render's free tier blocks all outbound SMTP ports (25, 465, 587), so a
 * plain-HTTPS API is the only delivery path that works without a paid plan.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:KoderHyre <onboarding@resend.dev>}")
    private String fromAddress;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.resend.com")
            .build();

    /** Returns true if the email was accepted by Resend for delivery. */
    private boolean send(String to, String subject, String text) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.info("Email to {} — \"{}\" (RESEND_API_KEY not configured)", to, subject);
            System.out.println("Email to " + to + ": " + subject + "\n" + text);
            return false;
        }
        try {
            restClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + resendApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", fromAddress,
                            "to", List.of(to),
                            "subject", subject,
                            "text", text
                    ))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent email to {} — {}", to, subject);
            return true;
        } catch (RestClientException ex) {
            log.error("Failed to send email to {}: {}", to, ex.getMessage());
            return false;
        }
    }

    public void sendPasswordReset(String to, String resetLink) {
        boolean sent = send(to, "Reset your KoderHyre password",
                "Click the link below to reset your password. It expires in 1 hour.\n\n" + resetLink
                        + "\n\nIf you did not request this, ignore this email.");
        if (!sent) {
            System.out.println("Password reset link for " + to + ": " + resetLink);
        }
    }

    public void sendInviteNotification(String to, String candidateName,
                                        String jobTitle, String companyName,
                                        String dashboardUrl) {
        send(to, "You have a new interview invitation — " + jobTitle + " at " + companyName,
                "Hi " + candidateName + ",\n\n"
                        + companyName + " has invited you to interview for the role of " + jobTitle + ".\n\n"
                        + "Log in to your dashboard to review and respond:\n" + dashboardUrl + "\n\n"
                        + "— The KoderHyre Team");
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
        send(to, "Update on your application — " + jobTitle + " at " + companyName,
                "Hi " + candidateName + ",\n\n"
                        + companyName + " has updated your application status for " + jobTitle + ".\n\n"
                        + "New status: " + friendlyStatus + "\n\n"
                        + "View your applications:\n" + dashboardUrl + "\n\n"
                        + "— The KoderHyre Team");
    }

    public void sendProfileApproved(String to, String candidateName) {
        send(to, "Your profile has been approved — KoderHyre",
                "Hi " + candidateName + ",\n\n"
                        + "Great news! Our team has reviewed your profile and it has been approved.\n\n"
                        + "You are now visible to top recruiters on KoderHyre. "
                        + "Recruiters will reach out to you directly with interview opportunities.\n\n"
                        + "Make sure your profile and resume are up to date to maximise your chances.\n\n"
                        + "— The KoderHyre Team");
    }

    public void sendProfileRejected(String to, String candidateName, String reason) {
        send(to, "Update on your KoderHyre profile review",
                "Hi " + candidateName + ",\n\n"
                        + "Thank you for applying to join KoderHyre's curated talent pool.\n\n"
                        + "After reviewing your profile, we are unable to approve it at this time.\n\n"
                        + (reason != null && !reason.isBlank() ? "Reason: " + reason + "\n\n" : "")
                        + "You are welcome to update your profile and reapply after 30 days.\n\n"
                        + "— The KoderHyre Team");
    }

    /**
     * Sent to CANDIDATE when they accept an interview invite.
     * Confirms acceptance and tells them the recruiter will reach out.
     */
    public void sendAcceptanceConfirmationToCandidate(String to, String candidateName,
                                                       String jobTitle, String companyName,
                                                       String requiredSkills, String dashboardUrl) {
        send(to, "Interview confirmed — " + jobTitle + " at " + companyName,
                "Hi " + candidateName + ",\n\n"
                        + "You've accepted the interview invitation for " + jobTitle + " at " + companyName + ".\n\n"
                        + "The hiring team will be in touch with you shortly to schedule the interview.\n\n"
                        + "In the meantime, prepare to discuss your experience with:\n"
                        + requiredSkills + "\n\n"
                        + "View all your interview requests here:\n" + dashboardUrl + "\n\n"
                        + "Best of luck!\n— The KoderHyre Team");
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
        send(to, candidateName + " has accepted your interview request — " + jobTitle,
                "Hi " + recruiterName + ",\n\n"
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
    }

    public void sendRecruiterApproved(String to, String recruiterName) {
        send(to, "Your recruiter account is approved — KoderHyre",
                "Hi " + recruiterName + ",\n\n"
                        + "Your recruiter account on KoderHyre has been approved!\n\n"
                        + "You can now log in and start posting jobs and browsing our curated talent pool.\n\n"
                        + "Log in at: [your domain]/recruiter/login\n\n"
                        + "— The KoderHyre Team");
    }

    public void sendRecruiterRejected(String to, String recruiterName) {
        send(to, "Update on your KoderHyre recruiter account",
                "Hi " + recruiterName + ",\n\n"
                        + "Thank you for registering as a recruiter on KoderHyre.\n\n"
                        + "After reviewing your account details, we are unable to approve your account at this time.\n\n"
                        + "If you believe this is an error or would like to provide more information, "
                        + "please contact us at support@koderhyre.tech.\n\n"
                        + "— The KoderHyre Team");
    }

    public void sendOtp(String to, String otp) {
        boolean sent = send(to, "Your Verification OTP",
                "Your OTP is: " + otp + ". It is valid for 10 minutes.");
        if (!sent) {
            // OTP must still be usable even if delivery failed — never rethrow here.
            log.info("Falling back — OTP for {} is {}", to, otp);
            System.out.println("OTP for " + to + " is " + otp);
        }
    }
}
