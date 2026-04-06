
package com.example.repositary.recruiter;

import com.example.model.Jobseeker;
import com.example.model.recruiter.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Optional<Recruiter> findByEmail(String email);

    @Query("SELECT j FROM Jobseeker j WHERE j.id = :id")
    Optional<Jobseeker> findJobseekerById(@Param("id") Long id);
}
