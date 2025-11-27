package com.example.service;

import com.example.model.Job;
import com.example.model.JobseekerProfile;
import com.example.repositary.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobRecommendationService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> getRecommendedJobs(JobseekerProfile profile) {
        if (profile == null || profile.getSkills() == null || profile.getSkills().isEmpty()) {
            // Return some default jobs if no skills are available
            return jobRepository.findByActiveTrue().stream()
                    .limit(4)
                    .collect(Collectors.toList());
        }

        // Parse skills from comma-separated string
        List<String> skills = Arrays.asList(profile.getSkills().split(","));

        // Get matching jobs for each skill and merge results
        Set<Job> recommendedJobs = new LinkedHashSet<>();

        for (String skill : skills) {
            if (skill != null && !skill.trim().isEmpty()) {
                List<Job> matchingJobs = jobRepository.findMatchingJobs(skill.trim());
                recommendedJobs.addAll(matchingJobs);

                // Limit to 4 recommendations
                if (recommendedJobs.size() >= 4) {
                    break;
                }
            }
        }

        return new ArrayList<>(recommendedJobs);
    }
}