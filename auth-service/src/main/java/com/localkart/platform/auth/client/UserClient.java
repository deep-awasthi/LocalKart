package com.localkart.platform.auth.client;

import com.localkart.platform.auth.web.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8082}")
public interface UserClient {

    @PostMapping("/api/v1/users")
    void createUserProfile(@RequestBody UserDto userDto);
}
