package com.localkart.platform.user.web.rest;

import com.localkart.platform.user.domain.UserProfile;
import com.localkart.platform.user.service.UserProfileService;
import com.localkart.platform.user.web.dto.UserResponse;
import com.localkart.platform.shared.annotation.IgnoreResponseWrapping;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import com.localkart.platform.shared.security.SecurityContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserProfileService profileService;

    @PostMapping
    public UserResponse createUserProfile(@Valid @RequestBody UserProfile profile) {
        log.info("REST request to create user profile: {}", profile.getEmail());
        UserProfile created = profileService.createUserProfile(profile);
        return mapToResponse(created);
    }

    @GetMapping("/me")
    public UserResponse getMyProfile() {
        String email = SecurityContextUtils.getUsername();
        log.info("REST request to fetch profile for current user: {}", email);
        if (email == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        UserProfile profile = profileService.getUserProfileByEmail(email);
        return mapToResponse(profile);
    }

    @PutMapping("/me/address")
    public UserResponse updateMyAddress(@RequestParam("address") String address) {
        String email = SecurityContextUtils.getUsername();
        log.info("REST request to update address for user: {}", email);
        if (email == null) {
            throw new BusinessException("User is not authenticated", ErrorCode.UNAUTHORIZED);
        }
        UserProfile updated = profileService.updateUserAddress(email, address);
        return mapToResponse(updated);
    }

    @IgnoreResponseWrapping
    @GetMapping("/internal/{email}")
    public UserResponse getProfileByEmail(@PathVariable("email") String email) {
        log.info("Internal request to fetch profile for user: {}", email);
        UserProfile profile = profileService.getUserProfileByEmail(email);
        return mapToResponse(profile);
    }

    private UserResponse mapToResponse(UserProfile profile) {
        return UserResponse.builder()
                .id(profile.getId())
                .email(profile.getEmail())
                .name(profile.getName())
                .phone(profile.getPhone())
                .roles(profile.getRoles())
                .address(profile.getAddress())
                .build();
    }
}
