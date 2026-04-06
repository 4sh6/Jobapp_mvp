package com.example.service;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.JobseekerRegistrationDto;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.repositary.JobseekerProfileRepository;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobseekerService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private JobseekerProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Jobseeker registerJobseeker(JobseekerRegistrationDto dto) {
        // Normalize email (trim + lowercase) to avoid duplicates due to case/whitespace
        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        // Check if email already exists to avoid unique constraint violation
        Optional<Jobseeker> existing = jobseekerRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + normalizedEmail);
        }

        Jobseeker jobseeker = new Jobseeker();
        jobseeker.setFullName(dto.getFullName());
        jobseeker.setEmail(normalizedEmail);
        jobseeker.setPassword(passwordEncoder.encode(dto.getPassword()));
        try {
            return jobseekerRepository.save(jobseeker);
        } catch (DataIntegrityViolationException ex) {
            // in case of a race condition someone inserted the same email between the check and save
            throw new IllegalArgumentException("Email already in use: " + normalizedEmail);
        }
    }

    public Optional<Jobseeker> findByEmail(String email) {
        return jobseekerRepository.findByEmail(email);
    }

    @Transactional
    public JobseekerProfile updateProfile(Jobseeker jobseeker, JobseekerProfileDto dto) {
        JobseekerProfile profile = profileRepository.findById(jobseeker.getId())
                .orElseGet(() -> {
                    JobseekerProfile p = new JobseekerProfile();
                    p.setJobseeker(jobseeker);
                    p.setId(jobseeker.getId());
                    return p;
                });

        // Basic details
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
        profile.setWorkMode(dto.getWorkMode());
        profile.setNoticePeriodDays(dto.getNoticePeriodDays());

        jobseeker.setProfileCompleted(true);
        jobseekerRepository.save(jobseeker);
        return profileRepository.save(profile);
    }
}