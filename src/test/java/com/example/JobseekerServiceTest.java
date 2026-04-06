
package com.example;

import com.example.dto.JobseekerProfileDto;
import com.example.dto.JobseekerRegistrationDto;
import com.example.model.Jobseeker;
import com.example.model.JobseekerProfile;
import com.example.repositary.JobseekerProfileRepository;
import com.example.repositary.JobseekerRepository;
import com.example.service.JobseekerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class JobseekerServiceTest {

    @Test
    void registerJobseeker_encodesPasswordAndSaves() {
        JobseekerRepository repo = mock(JobseekerRepository.class);
        JobseekerProfileRepository profileRepo = mock(JobseekerProfileRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        when(encoder.encode("plainpass")).thenReturn("encoded");
        when(repo.save(any(Jobseeker.class))).thenAnswer(inv -> inv.getArgument(0));

        JobseekerService service = new JobseekerService();
        // inject via reflection for brevity
        TestUtils.setField(service, "jobseekerRepository", repo);
        TestUtils.setField(service, "profileRepository", profileRepo);
        TestUtils.setField(service, "passwordEncoder", encoder);

        JobseekerRegistrationDto dto = new JobseekerRegistrationDto();
        dto.setFullName("John Doe");
        dto.setEmail("john@example.com");
        dto.setPassword("plainpass");

        Jobseeker saved = service.registerJobseeker(dto);

        ArgumentCaptor<Jobseeker> captor = ArgumentCaptor.forClass(Jobseeker.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
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
