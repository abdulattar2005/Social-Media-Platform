package com.socialmedia.userservice.controller;

import com.socialmedia.userservice.dto.UserDTO;
import com.socialmedia.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserDTO getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/username/{username}")
    public UserDTO getByUsername(@PathVariable String username) {
        return userService.getByUsername(username);
    }

    @GetMapping("/search")
    public List<UserDTO> search(@RequestParam String q) {
        return userService.search(q);
    }

    @PostMapping("/{id}/follow")
    public void follow(@PathVariable Long id, @RequestHeader("X-Auth-User") String currentUsername,
                        @RequestParam Long followerId) {
        userService.follow(followerId, id);
    }

    @DeleteMapping("/{id}/follow")
    public void unfollow(@PathVariable Long id, @RequestParam Long followerId) {
        userService.unfollow(followerId, id);
    }

    /** Called internally by notification-service to fan out "new post" alerts. */
    @GetMapping("/{id}/followers")
    public List<UserDTO> getFollowers(@PathVariable Long id) {
        return userService.getFollowers(id);
    }

    @GetMapping("/{id}/following")
    public List<UserDTO> getFollowing(@PathVariable Long id) {
        return userService.getFollowing(id);
    }
}
