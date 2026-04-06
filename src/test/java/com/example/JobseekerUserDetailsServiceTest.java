
package com.example;

import com.example.model.Jobseeker;
import com.example.repositary.JobseekerRepository;
import com.example.service.JobseekerUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class JobseekerUserDetailsServiceTest {

    @Test
    void loadUserByUsername_blocksUnverified() {
        JobseekerRepository repo = mock(JobseekerRepository.class);
        Jobseeker js = new Jobseeker();
        js.setEmail("john@example.com");
        js.setEmailVerified(false);
        when(repo.findByEmail("john@example.com")).thenReturn(Optional.of(js));

        JobseekerUserDetailsService uds = new JobseekerUserDetailsService();
        TestUtils.setField(uds, "jobseekerRepository", repo);

        assertThatThrownBy(() -> uds.loadUserByUsername("john@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_returnsUserDetailsForVerified() {
        JobseekerRepository repo = mock(JobseekerRepository.class);
        Jobseeker js = new Jobseeker();
        js.setEmail("john@example.com");
        js.setPassword("encoded");
        js.setEmailVerified(true);
        when(repo.findByEmail("john@example.com")).thenReturn(Optional.of(js));

        JobseekerUserDetailsService uds = new JobseekerUserDetailsService();
        TestUtils.setField(uds, "jobseekerRepository", repo);

        UserDetails ud = uds.loadUserByUsername("john@example.com");
        assert ud.getUsername().equals("john@example.com");
    }
}
