
package com.example;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.JobseekerRegistrationDto;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.model.PendingJobseekerRegistration;
import com.example.repository.JobseekerProfileRepository;
import com.example.repository.JobseekerRepository;
import com.example.repository.PendingJobseekerRegistrationRepository;
import com.example.service.JobseekerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JobseekerServiceTest {

    @Test
    void registerJobseeker_encodesPasswordAndStashesPendingRegistration() {
        JobseekerRepository repo = mock(JobseekerRepository.class);
        JobseekerProfileRepository profileRepo = mock(JobseekerProfileRepository.class);
        PendingJobseekerRegistrationRepository pendingRepo = mock(PendingJobseekerRegistrationRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(encoder.encode("plainpass")).thenReturn("encoded");
        when(repo.findByEmail("john@example.com")).thenReturn(Optional.empty());

        JobseekerService service = new JobseekerService();
        // inject via reflection for brevity
        TestUtils.setField(service, "jobseekerRepository", repo);
        TestUtils.setField(service, "profileRepository", profileRepo);
        TestUtils.setField(service, "pendingRegistrationRepository", pendingRepo);
        TestUtils.setField(service, "passwordEncoder", encoder);

        JobseekerRegistrationDto dto = new JobseekerRegistrationDto();
        dto.setFullName("John Doe");
        dto.setEmail("john@example.com");
        dto.setPassword("plainpass");

        // No Jobseeker row should be created yet — only after OTP verification
        String email = service.registerJobseeker(dto);

        ArgumentCaptor<PendingJobseekerRegistration> captor = ArgumentCaptor.forClass(PendingJobseekerRegistration.class);
        verify(pendingRepo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(captor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(email).isEqualTo("john@example.com");
        verify(repo, never()).save(any(Jobseeker.class));
    }

    @Test
    void updateProfile_createsOrUpdatesProfile() {
        JobseekerRepository repo = mock(JobseekerRepository.class);
        JobseekerProfileRepository profileRepo = mock(JobseekerProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        JobseekerService service = new JobseekerService();
        TestUtils.setField(service, "jobseekerRepository", repo);
        TestUtils.setField(service, "profileRepository", profileRepo);
        TestUtils.setField(service, "passwordEncoder", encoder);

        Jobseeker js = new Jobseeker();
        js.setId(1L);

        when(profileRepo.findById(1L)).thenReturn(Optional.empty());
        when(profileRepo.save(any(JobseekerProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        JobseekerProfileDto dto = new JobseekerProfileDto();
        dto.setSkills("Java,Spring");
        dto.setExperienceYears(3);

        JobseekerProfile profile = service.updateProfile(js, dto);
        assertThat(profile.getSkills()).contains("Java");
        verify(repo).save(js);
    }
}
