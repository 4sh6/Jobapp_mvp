package com.example.service;

import com.example.model.Job;
import com.example.model.JobseekerProfile;
import com.example.repositary.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobRecommendationService {

    @Autowired
    private JobRepository jobRepository;

    public List<Job> recommendJobs(JobseekerProfile profile) {
        return jobRepository.listActiveJobs(Pageable.unpaged()).getContent();
    }
}