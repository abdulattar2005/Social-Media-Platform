package com.socialmedia.notificationservice.controller;

import com.socialmedia.notificationservice.dto.NotificationDTO;
import com.socialmedia.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{userId}")
    public List<NotificationDTO> getNotifications(@PathVariable Long userId) {
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/{userId}/unread-count")
    public Map<String, Long> getUnreadCount(@PathVariable Long userId) {
        return Map.of("unread", notificationService.getUnreadCount(userId));
    }

    @PutMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    /** Internal endpoint used by user-service (or a future follow event) to raise a "new follower" alert. */
    @PostMapping("/internal/new-follower")
    public void newFollowerNotification(@RequestParam Long followedUserId, @RequestParam String followerUsername) {
        notificationService.notifyNewFollower(followedUserId, followerUsername);
    }
}
