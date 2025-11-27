package com.example.repositary;

import com.example.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByActiveTrue();

    @Query(value = "SELECT * FROM jobs j WHERE j.active = true AND " +
            "(LOWER(j.required_skills) LIKE LOWER(CONCAT('%', :skill, '%'))) " +
            "ORDER BY j.posted_date DESC LIMIT 10",
            nativeQuery = true)
    List<Job> findMatchingJobs(@Param("skill") String skill);
}