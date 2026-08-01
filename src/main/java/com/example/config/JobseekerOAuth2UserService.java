package com.example.config;

import com.example.model.Jobseeker;
import com.example.repository.JobseekerRepository;
import com.example.service.JobseekerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Google's registered scope includes "openid", making this an OIDC login — Spring routes
// those through oidcUserService(), not userService(), so this must extend OidcUserService
// or our custom Jobseeker lookup/creation and ROLE_JOBSEEKER grant are silently skipped.
@Service
public class JobseekerOAuth2UserService extends OidcUserService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JobseekerService jobseekerService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        Map<String, Object> attrs = oidcUser.getAttributes();
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

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_JOBSEEKER")),
                userRequest.getIdToken(),
                oidcUser.getUserInfo(),
                "email" // use email as key for getName()
        );
    }
}
