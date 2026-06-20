package com.localkart.platform.user.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.localkart.platform.shared.advice.ApiResponseWrapperAdvice;
import com.localkart.platform.shared.exception.GlobalExceptionHandler;
import com.localkart.platform.user.config.SecurityConfig;
import com.localkart.platform.user.domain.UserProfile;
import com.localkart.platform.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ApiResponseWrapperAdvice.class})
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserProfileService profileService;

    @Test
    void testCreateProfileValidationFailure() throws Exception {
        UserProfile invalidProfile = UserProfile.builder()
                .email("invalid-email")
                .name("")
                .phone("")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VAL-400"));
    }

    @Test
    void testCreateProfileSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .email("john@example.com")
                .name("John")
                .phone("123456")
                .roles("ROLE_USER")
                .build();

        when(profileService.createUserProfile(any(UserProfile.class))).thenReturn(profile);

        mockMvc.perform(post("/api/v1/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.name").value("John"));
    }

    @Test
    void testGetMyProfileSuccess() throws Exception {
        UserProfile profile = UserProfile.builder()
                .email("jane@example.com")
                .name("Jane")
                .phone("654321")
                .roles("ROLE_USER")
                .build();

        when(profileService.getUserProfileByEmail("jane@example.com")).thenReturn(profile);

        mockMvc.perform(get("/api/v1/users/me")
                        .header("X-Auth-User", "jane@example.com")
                        .header("X-Auth-Roles", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane@example.com"))
                .andExpect(jsonPath("$.data.name").value("Jane"));
    }
}
