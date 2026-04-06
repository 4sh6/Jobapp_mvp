
package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicHomeIsAccessible() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void jobseekerDashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/jobseeker/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void recruiterDashboardRequiresAuth() throws Exception {
        mockMvc.perform(get("/recruiter/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}
