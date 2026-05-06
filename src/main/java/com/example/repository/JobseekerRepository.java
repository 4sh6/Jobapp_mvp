
package com.example.repository;

import com.example.model.Jobseeker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JobseekerRepository extends JpaRepository<Jobseeker, Long> {
    Optional<Jobseeker> findByEmail(String email);

    Optional<Jobseeker> findByReferralCode(String referralCode);

    @Query("SELECT j FROM Jobseeker j WHERE j.approvalStatus = :status " +
           "OR (:status = 'PENDING' AND j.approvalStatus IS NULL) " +
           "ORDER BY j.createdAt DESC")
    List<Jobseeker> findByApprovalStatusOrderByCreatedAtDesc(@Param("status") String approvalStatus);

    @Query("SELECT COUNT(j) FROM Jobseeker j WHERE j.approvalStatus = :status " +
           "OR (:status = 'PENDING' AND j.approvalStatus IS NULL)")
    long countByApprovalStatus(@Param("status") String approvalStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Jobseeker j SET j.approvalStatus = 'PENDING' WHERE j.approvalStatus IS NULL")
    void backfillNullApprovalStatus();
}
