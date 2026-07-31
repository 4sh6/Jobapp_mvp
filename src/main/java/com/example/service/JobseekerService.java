package com.example.service;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.JobseekerRegistrationDto;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.model.PendingJobseekerRegistration;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.PendingJobseekerRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class JobseekerService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JobseekerProfileRepository profileRepository;

    @Autowired
    private PendingJobseekerRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ReferralService referralService;

    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Validates the registration and stashes it as a pending registration until the OTP is
     * verified — no Jobseeker row (and therefore no admin-portal visibility) is created yet.
     * Returns the normalized email to send the OTP to.
     */
    @Transactional
    public String registerJobseeker(JobseekerRegistrationDto dto) {
        // Normalize email (trim + lowercase) to avoid duplicates due to case/whitespace
        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        Optional<Jobseeker> existing = jobseekerRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            Jobseeker existingUser = existing.get();
            if (existingUser.isEmailVerified()) {
                // Fully registered — check if REJECTED with active 30-day cooldown
                if ("REJECTED".equals(existingUser.getApprovalStatus())
                        && existingUser.getRejectedAt() != null
                        && existingUser.getRejectedAt().isAfter(LocalDateTime.now().minusDays(30))) {
                    long daysLeft = 30 - java.time.temporal.ChronoUnit.DAYS.between(
                            existingUser.getRejectedAt(), LocalDateTime.now());
                    throw new IllegalArgumentException(
                            "Your profile was rejected. You can re-register after " + daysLeft + " more day(s).");
                }
                if (!"REJECTED".equals(existingUser.getApprovalStatus())) {
                    throw new IllegalArgumentException("Email already in use: " + normalizedEmail);
                }
                // REJECTED and cooldown passed — allow re-registration: reset the existing record
                existingUser.setFullName(dto.getFullName());
                existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
                existingUser.setApprovalStatus("PENDING");
                existingUser.setRejectionReason(null);
                existingUser.setRejectedAt(null);
                existingUser.setEmailVerified(false);
                jobseekerRepository.save(existingUser);
                return normalizedEmail;
            }
            // Unverified row from before the pending-registration flow existed — overwrite it
            // directly so the user can retry without being permanently blocked
            existingUser.setFullName(dto.getFullName());
            existingUser.setPassword(passwordEncoder.encode(dto.getPassword()));
            jobseekerRepository.save(existingUser);
            return normalizedEmail;
        }

        // No account exists yet — hold the data until OTP verification succeeds instead of
        // creating the Jobseeker row now.
        pendingRegistrationRepository.deleteByEmail(normalizedEmail); // clear any abandoned earlier attempt
        pendingRegistrationRepository.save(new PendingJobseekerRegistration(
                normalizedEmail,
                dto.getFullName(),
                passwordEncoder.encode(dto.getPassword()),
                dto.getReferredByCode()));
        return normalizedEmail;
    }

    /**
     * Called once the OTP is confirmed. Creates the Jobseeker row now (first time it appears
     * anywhere, including the admin portal) if this was a pending registration, or simply flips
     * the verified flag if the row already existed (legacy accounts pre-dating this flow).
     */
    @Transactional
    public Jobseeker confirmEmailVerified(String email) {
        Optional<Jobseeker> existing = jobseekerRepository.findByEmail(email);
        if (existing.isPresent()) {
            Jobseeker jobseeker = existing.get();
            jobseeker.setEmailVerified(true);
            jobseeker.setVerifiedAt(LocalDateTime.now());
            if (jobseeker.getReferralCode() == null || jobseeker.getReferralCode().isBlank()) {
                jobseeker.setReferralCode(generateUniqueReferralCode());
            }
            return jobseekerRepository.save(jobseeker);
        }

        PendingJobseekerRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("No pending registration found for " + email));

        Jobseeker jobseeker = new Jobseeker();
        jobseeker.setFullName(pending.getFullName());
        jobseeker.setEmail(pending.getEmail());
        jobseeker.setPassword(pending.getPassword());
        jobseeker.setReferralCode(generateUniqueReferralCode());
        jobseeker.setEmailVerified(true);
        jobseeker.setVerifiedAt(LocalDateTime.now());

        Jobseeker saved;
        try {
            saved = jobseekerRepository.save(jobseeker);
        } catch (DataIntegrityViolationException ex) {
            // race condition — another request inserted the same email between check and save
            throw new IllegalArgumentException("Email already in use: " + email);
        }
        linkReferral(pending.getReferredByCode(), saved);
        pendingRegistrationRepository.deleteByEmail(email);
        return saved;
    }

    private void linkReferral(String referredByCode, Jobseeker referee) {
        if (referredByCode == null || referredByCode.isBlank()) return;
        jobseekerRepository.findByReferralCode(referredByCode.trim().toUpperCase())
                .ifPresent(referrer -> referralService.createReferral(referrer, referee));
    }

    /** Ensures the jobseeker has a referral code, generating one if missing (e.g. Google signups). */
    @Transactional
    public String ensureReferralCode(Jobseeker jobseeker) {
        if (jobseeker.getReferralCode() == null || jobseeker.getReferralCode().isBlank()) {
            jobseeker.setReferralCode(generateUniqueReferralCode());
            jobseekerRepository.save(jobseeker);
        }
        return jobseeker.getReferralCode();
    }

    /** Generates a unique 8-char alphanumeric referral code (e.g. NXA1B2C3). */
    private String generateUniqueReferralCode() {
        for (int attempts = 0; attempts < 10; attempts++) {
            StringBuilder sb = new StringBuilder("NX");
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_CHARS.charAt(SECURE_RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (jobseekerRepository.findByReferralCode(code).isEmpty()) return code;
        }
        // Fallback: highly unlikely but use timestamp suffix for uniqueness
        return "NX" + Long.toHexString(System.currentTimeMillis()).toUpperCase().substring(0, 6);
    }

    public Optional<Jobseeker> findByEmail(String email) {
        return jobseekerRepository.findByEmail(email);
    }

    @Transactional
    public JobseekerProfile updateProfile(Jobseeker jobseeker, JobseekerProfileDto dto) {
        JobseekerProfile profile = profileRepository.findById(jobseeker.getId())
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker); // @MapsId derives ID automatically — do NOT set manually
                    return p;
                });

        // Basic details
        profile.setMobileNumber(dto.getMobileNumber());
        profile.setProfileHeadline(dto.getProfileHeadline());
        profile.setAbout(dto.getAbout());

        // Professional Experience
        profile.setExperienceYears(dto.getExperienceYears());
        profile.setExperienceMonths(dto.getExperienceMonths());
        profile.setCurrentCompany(dto.getCurrentCompany());
        profile.setPastCompanies(dto.getPastCompanies());
        profile.setCurrentRole(dto.getCurrentRole());
        profile.setCurrentCtc(dto.getCurrentCtc());
        profile.setExpectedCtc(dto.getExpectedCtc());
        profile.setSkills(dto.getSkills());
        profile.setPrimarySkills(dto.getPrimarySkills());

        // Education
        profile.setHighestEducation(dto.getHighestEducation());
        profile.setInstitution(dto.getInstitution());
        profile.setFieldOfStudy(dto.getFieldOfStudy());
        profile.setGraduationYear(dto.getGraduationYear());

        // Job Preferences
        profile.setPreferredLocations(dto.getPreferredLocations());
        profile.setWorkMode(dto.getWorkMode() != null ? String.join(", ", dto.getWorkMode()) : null);
        profile.setNoticePeriodDays(dto.getNoticePeriodDays());
        profile.setServingNotice(dto.getServingNotice());
        profile.setNoticeStartDate(dto.getNoticeStartDate());

        jobseeker.setProfileCompleted(true);
        jobseekerRepository.save(jobseeker);
        return profileRepository.save(profile);
    }
}