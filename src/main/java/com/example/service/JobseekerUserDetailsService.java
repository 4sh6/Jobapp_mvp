package com.example.service;

import com.example.model.Jobseeker;
import com.example.repositary.JobseekerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class JobseekerUserDetailsService implements UserDetailsService {

    @Autowired
    private JobseekerRepository jobseekerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Jobseeker jobseeker = jobseekerRepository.findByEmail(email);
        if (jobseeker == null) {
            throw new UsernameNotFoundException("Jobseeker not found with email: " + email);
        }

        return new User(
                jobseeker.getEmail(),
                jobseeker.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_JOBSEEKER"))
        );
    }
}