package com.example.service;

import com.example.model.Jobseeker;
import com.example.model.Referral;
import com.example.repository.ReferralRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReferralService {

    private static final Logger log = LoggerFactory.getLogger(ReferralService.class);

    @Autowired private ReferralRepository referralRepository;

    /** Called when a new jobseeker registers via a referral link. */
    @Transactional
    public void createReferral(Jobseeker referrer, Jobseeker referee) {
        // Guard: don't create duplicate referrals
        if (referralRepository.findByReferee(referee).isPresent()) return;
        // Guard: referrer cannot refer themselves
        if (referrer.getId().equals(referee.getId())) return;

        Referral referral = new Referral();
        referral.setReferrer(referrer);
        referral.setReferee(referee);
        referral.setStatus("SIGNED_UP");
        referralRepository.save(referral);
        log.info("Referral created: referrer={} referee={}", referrer.getEmail(), referee.getEmail());
    }

    /** Called when admin approves a jobseeker's profile. Advances SIGNED_UP → PROFILE_APPROVED. */
    @Transactional
    public void onProfileApproved(Jobseeker referee) {
        referralRepository.findByReferee(referee).ifPresent(r -> {
            if ("SIGNED_UP".equals(r.getStatus())) {
                r.setStatus("PROFILE_APPROVED");
                referralRepository.save(r);
                log.info("Referral advanced to PROFILE_APPROVED for referee={}", referee.getEmail());
            }
        });
    }

    /** Called when recruiter marks an application as HIRED. Advances any earlier status → HIRED. */
    @Transactional
    public void onHired(Jobseeker referee) {
        referralRepository.findByReferee(referee).ifPresent(r -> {
            if (!"HIRED".equals(r.getStatus()) && !"REWARD_PAID".equals(r.getStatus())) {
                r.setStatus("HIRED");
                r.setHiredAt(LocalDateTime.now());
                referralRepository.save(r);
                log.info("Referral advanced to HIRED for referee={}", referee.getEmail());
            }
        });
    }

    /** Called by admin when the ₹5,000 reward has been paid to the referrer. */
    @Transactional
    public boolean markRewardPaid(Long referralId) {
        Optional<Referral> opt = referralRepository.findById(referralId);
        if (opt.isEmpty()) return false;
        Referral r = opt.get();
        if (!"HIRED".equals(r.getStatus())) return false; // can only pay after HIRED
        r.setStatus("REWARD_PAID");
        r.setRewardPaidAt(LocalDateTime.now());
        referralRepository.save(r);
        log.info("Referral reward marked PAID for referral id={}", referralId);
        return true;
    }

    public List<Referral> getReferralsByReferrer(Jobseeker referrer) {
        return referralRepository.findByReferrerOrderByCreatedAtDesc(referrer);
    }

    public List<Referral> getAllReferrals() {
        return referralRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Referral> getPendingRewards() {
        return referralRepository.findByStatusOrderByCreatedAtDesc("HIRED");
    }
}
