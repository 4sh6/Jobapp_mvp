package com.example.service;

import com.example.model.Job;
import com.example.model.JobseekerProfile;
import com.example.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class JobRecommendationService {

    @Autowired
    private JobRepository jobRepository;

    /**
     * Returns up to 10 recommended jobs for a jobseeker profile.
     * Strategy: match each of the candidate's skills against job requiredSkills/title,
     * deduplicate, then pad with latest jobs if fewer than 5 matches found.
     */
    public List<Job> recommendJobs(JobseekerProfile profile) {
        if (profile == null) {
            return jobRepository.listActiveJobs(PageRequest.of(0, 10)).getContent();
        }

        // Collect all skills from both fields
        List<String> skills = new ArrayList<>();
        addSkills(skills, profile.getPrimarySkills());
        addSkills(skills, profile.getSkills());

        if (skills.isEmpty()) {
            return jobRepository.listActiveJobs(PageRequest.of(0, 10)).getContent();
        }

        Set<Long> seenIds = new LinkedHashSet<>();
        List<Job> result = new ArrayList<>();

        for (String skill : skills) {
            if (result.size() >= 10) break;
            List<Job> matches = jobRepository.filterJobs(
                    skill, null, null, null, null, null, null, PageRequest.of(0, 10)).getContent();
            for (Job job : matches) {
                if (seenIds.add(job.getId())) {
                    result.add(job);
                    if (result.size() >= 10) break;
                }
            }
        }

        // Pad with latest jobs if fewer than 5 skill-based matches
        if (result.size() < 5) {
            List<Job> latest = jobRepository.listActiveJobs(PageRequest.of(0, 10)).getContent();
            for (Job job : latest) {
                if (seenIds.add(job.getId())) {
                    result.add(job);
                    if (result.size() >= 10) break;
                }
            }
        }

        return result;
    }

    private void addSkills(List<String> target, String csv) {
        if (csv == null || csv.isBlank()) return;
        for (String s : csv.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isBlank()) target.add(trimmed);
        }
    }
}