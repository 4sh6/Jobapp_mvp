package com.example.repository;

import com.example.model.Jobseeker;
import com.example.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    List<Referral> findByReferrerOrderByCreatedAtDesc(Jobseeker referrer);

    Optional<Referral> findByReferee(Jobseeker referee);

    List<Referral> findByStatusOrderByCreatedAtDesc(String status);

    List<Referral> findAllByOrderByCreatedAtDesc();

    void deleteByReferrer(Jobseeker referrer);

    void deleteByReferee(Jobseeker referee);
}
