package com.example.service;

import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import org.springframework.stereotype.Service;

@Service
public class OnboardingService {

    /**
     * Calculates profile completion percentage (0-100) based on filled fields.
     * Weights: resume=20, skills=15, verified+experience+education+salary+locations+workmode=10 each,
     *          about=5.
     */
    public int getCompletionPercentage(Jobseeker jobseeker, JobseekerProfile profile) {
        int score = 0;

        if (jobseeker.isEmailVerified())              score += 10;
        if (jobseeker.isResumeUploaded())             score += 20;

        if (profile != null) {
            if (hasValue(profile.getPrimarySkills())
                    || hasValue(profile.getSkills()))  score += 15;
            if (profile.getExperienceYears() != null)  score += 10;
            if (hasValue(profile.getHighestEducation())) score += 10;
            if (profile.getExpectedCtc() != null)      score += 10;
            if (hasValue(profile.getPreferredLocations())) score += 10;
            if (hasValue(profile.getWorkMode()))       score += 10;
            if (hasValue(profile.getAbout()))          score += 5;
        }

        return Math.min(score, 100);
    }

    /** Backward-compatible overload used by legacy callers. */
    public int getCompletionPercentage(Jobseeker jobseeker) {
        return getCompletionPercentage(jobseeker, null);
    }

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }
}