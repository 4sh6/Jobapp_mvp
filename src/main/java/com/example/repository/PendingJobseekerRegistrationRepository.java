package com.example.repository;

import com.example.model.PendingJobseekerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingJobseekerRegistrationRepository extends JpaRepository<PendingJobseekerRegistration, Long> {

    Optional<PendingJobseekerRegistration> findByEmail(String email);

    void deleteByEmail(String email);
}
