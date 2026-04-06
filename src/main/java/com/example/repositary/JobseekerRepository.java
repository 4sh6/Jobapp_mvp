
package com.example.repositary;

import com.example.model.Jobseeker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobseekerRepository extends JpaRepository<Jobseeker, Long> {
    Optional<Jobseeker> findByEmail(String email);
}
