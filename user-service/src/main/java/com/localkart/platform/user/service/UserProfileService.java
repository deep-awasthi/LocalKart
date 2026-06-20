package com.localkart.platform.user.service;

import com.localkart.platform.user.domain.UserProfile;

public interface UserProfileService {
    UserProfile createUserProfile(UserProfile profile);
    UserProfile getUserProfileByEmail(String email);
    UserProfile updateUserAddress(String email, String address);
}
