package com.socialmedia.notificationservice.service;

import com.socialmedia.notificationservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Feign client resolved via Eureka service discovery (logical name
 * "user-service" - no hard-coded host:port). Used to fetch the follower
 * list of a post's author when a post.created event arrives.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/{id}/followers")
    List<UserDTO> getFollowers(@PathVariable("id") Long userId);
}
