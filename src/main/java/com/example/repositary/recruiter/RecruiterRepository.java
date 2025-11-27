package com.example.repositary.recruiter;

import com.example.model.recruiter.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    // Find a recruiter by email (useful for login functionality)
    Recruiter findByEmail(String email);

    // Check if a recruiter exists with the given email (for validation)
    boolean existsByEmail(String email);
}