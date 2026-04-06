package com.example.repositary;

import com.example.model.Job;
import com.example.model.JobInvite;
import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobInviteRepository extends JpaRepository<JobInvite, Long> {

    List<JobInvite> findByJobseeker(Jobseeker jobseeker);

    boolean existsByJobAndJobseeker(Job job, Jobseeker jobseeker);

    List<JobInvite> findByJob_RecruiterOrderByCreatedAtDesc(Recruiter recruiter);

    long countByJob_Recruiter(Recruiter recruiter);
}