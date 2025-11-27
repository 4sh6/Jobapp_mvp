package com.example.repositary;

import com.example.model.JobseekerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobseekerProfileRepository extends JpaRepository<JobseekerProfile, Long> {
}