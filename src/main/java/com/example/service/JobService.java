package com.example.service;

import com.example.dto.JobDto;
import com.example.model.Job;
import com.example.model.recruiter.Recruiter;
import com.example.repositary.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private EventLogService eventLogService;

    @Transactional
    public Job createJob(JobDto dto, Recruiter recruiter) {
        Job job = new Job();
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setRequiredSkills(dto.getRequiredSkills());
        job.setLocation(dto.getLocation());
        job.setWorkMode(dto.getWorkMode());
        job.setJobType(dto.getJobType() != null ? dto.getJobType() : "FULL_TIME");
        job.setExperienceMin(dto.getExperienceMin());
        job.setExperienceMax(dto.getExperienceMax());
        job.setSalaryMin(dto.getSalaryMin());
        job.setSalaryMax(dto.getSalaryMax());
        job.setRecruiter(recruiter);
        job.setStatus("PUBLISHED");
        job.setActive(true);

        Job saved = jobRepository.save(job);
        eventLogService.log("job_created", saved.getId(), null, recruiter.getId(), null);
        return saved;
    }

    @Transactional
    public Job updateJob(Long id, JobDto dto, Recruiter recruiter) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        if (job.getRecruiter() == null || !job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to edit this job");
        }

        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setRequiredSkills(dto.getRequiredSkills());
        job.setLocation(dto.getLocation());
        job.setWorkMode(dto.getWorkMode());
        if (dto.getJobType() != null) job.setJobType(dto.getJobType());
        job.setExperienceMin(dto.getExperienceMin());
        job.setExperienceMax(dto.getExperienceMax());
        job.setSalaryMin(dto.getSalaryMin());
        job.setSalaryMax(dto.getSalaryMax());

        return jobRepository.save(job);
    }

    public Page<Job> listActiveJobs(Pageable pageable) {
        return jobRepository.findByActiveTrueAndStatus("PUBLISHED", pageable);
    }

    public Optional<Job> findById(Long id) {
        return jobRepository.findById(id);
    }

    public Page<Job> listJobsByRecruiter(Recruiter recruiter, Pageable pageable) {
        return jobRepository.findByRecruiter(recruiter, pageable);
    }

    @Transactional
    public void closeJob(Job job) {
        job.setStatus("CLOSED");
        job.setActive(false);
        jobRepository.save(job);
        if (job.getRecruiter() != null) {
            eventLogService.log("job_closed", job.getId(), null, job.getRecruiter().getId(), null);
        }
    }

    public Page<Job> search(String q, String workMode, Integer expMin, Integer expMax, Pageable pageable) {
        return jobRepository.filterJobs(
                (q != null && !q.isBlank()) ? q.trim() : null,
                (workMode != null && !workMode.isBlank()) ? workMode : null,
                expMin,
                expMax,
                pageable);
    }
}
