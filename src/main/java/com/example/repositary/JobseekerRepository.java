package com.example.repositary;

import com.example.model.Jobseeker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobseekerRepository extends JpaRepository<Jobseeker, Long> {
    // Find a jobseeker by their email
    Jobseeker findByEmail(String email);

    // Check if a jobseeker exists with this email
    boolean existsByEmail(String email);
}