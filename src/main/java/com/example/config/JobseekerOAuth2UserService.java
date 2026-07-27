package com.example.config;

import com.example.model.Jobseeker;
import com.example.repository.JobseekerRepository;
import com.example.service.JobseekerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobseekerOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JobseekerService jobseekerService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attrs = oAuth2User.getAttributes();
        // Google usually has "email" & "name"
        String email = (String) attrs.get("email");
        String name  = (String) attrs.getOrDefault("name", email);

        Jobseeker jobseeker = jobseekerRepository.findByEmail(email)
                .orElseGet(() -> {
                    Jobseeker js = new Jobseeker();
                    js.setEmail(email);
                    js.setFullName(name);
                    js.setEmailVerified(true);           // trust Google email
                    js.setVerifiedAt(LocalDateTime.now());
                    js.setAuthProvider("GOOGLE");
                    // optional: set random password
                    js.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));  // or encoder.encode(UUID...)
                    return jobseekerRepository.save(js);
                });

        // SECURITY: blocked accounts must not bypass the block via Google login
        if (!jobseeker.isActive()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_disabled"), "Account is disabled");
        }

        // Make sure existing LOCAL users that now use Google are marked verified
        if (!jobseeker.isEmailVerified()) {
            jobseeker.setEmailVerified(true);
            jobseeker.setVerifiedAt(LocalDateTime.now());
            jobseekerRepository.save(jobseeker);
        }

        // Google signups skip the normal registration path — make sure they get a referral code
        jobseekerService.ensureReferralCode(jobseeker);

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_JOBSEEKER")),
                attrs,
                "email" // use email as key for getName()
        );
    }
}
