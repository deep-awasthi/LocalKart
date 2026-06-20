package com.localkart.platform.user.service;

import com.localkart.platform.user.domain.UserProfile;
import com.localkart.platform.user.repository.UserProfileRepository;
import com.localkart.platform.user.service.impl.UserProfileServiceImpl;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTests {

    @Mock
    private UserProfileRepository repository;

    @InjectMocks
    private UserProfileServiceImpl service;

    @Test
    void testCreateProfileSuccess() {
        UserProfile profile = UserProfile.builder()
                .email("user@example.com")
                .name("Alice")
                .phone("111222333")
                .roles("ROLE_USER")
                .build();

        when(repository.existsByEmail(profile.getEmail())).thenReturn(false);
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.createUserProfile(profile);

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
        verify(repository, times(1)).save(profile);
    }

    @Test
    void testCreateProfileConflict() {
        UserProfile profile = UserProfile.builder().email("conflict@example.com").build();

        when(repository.existsByEmail(profile.getEmail())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.createUserProfile(profile));
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    void testGetProfileSuccess() {
        UserProfile profile = UserProfile.builder().email("test@example.com").name("Bob").build();

        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(profile));

        UserProfile result = service.getUserProfileByEmail("test@example.com");

        assertNotNull(result);
        assertEquals("Bob", result.getName());
    }

    @Test
    void testGetProfileNotFound() {
        when(repository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.getUserProfileByEmail("none@example.com"));
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void testUpdateAddressSuccess() {
        UserProfile profile = UserProfile.builder().email("test@example.com").address("Old Address").build();

        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);

        UserProfile result = service.updateUserAddress("test@example.com", "New Address");

        assertNotNull(result);
        assertEquals("New Address", result.getAddress());
        verify(repository, times(1)).save(profile);
    }
}
