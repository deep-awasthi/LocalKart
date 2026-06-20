package com.localkart.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.auth.client.UserClient;
import com.localkart.platform.auth.domain.UserCredentials;
import com.localkart.platform.auth.repository.UserCredentialsRepository;
import com.localkart.platform.auth.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegistrationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserCredentialsRepository credentialsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserClient userClient;

    @Test
    void testUserRegistrationFlowSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("jane.doe@example.com")
                .password("password123")
                .name("Jane Doe")
                .phone("1234567890")
                .build();

        // Stub user-service client profile creation response
        doNothing().when(userClient).createUserProfile(any());

        // Perform registration call
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"));

        // Verify credentials saved to H2 Database
        UserCredentials credentials = credentialsRepository.findByEmail("jane.doe@example.com").orElse(null);
        assertNotNull(credentials);
        assertTrue(passwordEncoder.matches("password123", credentials.getPassword()));
        assertEquals("jane.doe@example.com", credentials.getEmail());

        // Verify Feign client profile creation was triggered downstream
        verify(userClient, times(1)).createUserProfile(argThat(profile ->
                "jane.doe@example.com".equals(profile.getEmail()) &&
                "Jane Doe".equals(profile.getName()) &&
                "1234567890".equals(profile.getPhone()) &&
                "ROLE_USER".equals(profile.getRoles())
        ));
    }
}
