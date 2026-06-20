package com.localkart.platform.user.service.impl;

import com.localkart.platform.user.domain.UserProfile;
import com.localkart.platform.user.repository.UserProfileRepository;
import com.localkart.platform.user.service.UserProfileService;
import com.localkart.platform.shared.exception.BusinessException;
import com.localkart.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository repository;

    @Override
    @Transactional
    public UserProfile createUserProfile(UserProfile profile) {
        log.info("Creating profile for email: {}", profile.getEmail());
        if (repository.existsByEmail(profile.getEmail())) {
            throw new BusinessException("User profile already exists", ErrorCode.CONFLICT);
        }
        return repository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getUserProfileByEmail(String email) {
        log.info("Retrieving profile for email: {}", email);
        return repository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("User profile not found", ErrorCode.NOT_FOUND));
    }

    @Override
    @Transactional
    public UserProfile updateUserAddress(String email, String address) {
        log.info("Updating profile address for email: {}", email);
        UserProfile profile = getUserProfileByEmail(email);
        profile.setAddress(address);
        return repository.save(profile);
    }
}
