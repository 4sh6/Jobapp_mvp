package com.example.service.recruiter;

import com.example.model.recruiter.Recruiter;
import com.example.repository.recruiter.RecruiterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecruiterUserDetailsService implements UserDetailsService {

    @Autowired
    private RecruiterRepository recruiterRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Recruiter recruiter = recruiterRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Recruiter not found"));

        // Pending/Rejected/Suspended recruiters may still log in — the dashboard
        // shows them their status page, and all actions are gated by getActiveRecruiter().

        return new User(
                recruiter.getEmail(),
                recruiter.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_RECRUITER"))
        );
    }
}