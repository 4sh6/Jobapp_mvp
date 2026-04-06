
package com.example.service;

import com.example.model.Jobseeker;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobseekerUserDetailsService implements UserDetailsService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Jobseeker jobseeker = jobseekerRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Jobseeker not found"));
        if (!jobseeker.isEmailVerified()) {
            throw new UsernameNotFoundException("Email not verified");
        }
        return new User(jobseeker.getEmail(), jobseeker.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_JOBSEEKER")));
    }
}
