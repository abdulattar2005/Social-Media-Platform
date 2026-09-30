package com.socialmedia.notificationservice.service;

import com.socialmedia.notificationservice.dto.NotificationDTO;
import com.socialmedia.notificationservice.dto.UserDTO;
import com.socialmedia.notificationservice.model.Notification;
import com.socialmedia.notificationservice.repository.NotificationRepository;
import com.socialmedia.notificationservice.websocket.NotificationWebSocketHandler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final NotificationWebSocketHandler webSocketHandler;

    /**
     * Handles a PostCreatedEvent: resolves the author's followers (via
     * Feign -> user-service, guarded by a circuit breaker) then creates +
     * pushes one notification per follower.
     */
    public void notifyFollowersOfNewPost(Long authorId, String authorUsername, Long postId) {
        List<UserDTO> followers = getFollowersSafely(authorId);
        for (UserDTO follower : followers) {
            Notification notification = Notification.builder()
                    .userId(follower.getId())
                    .type("NEW_POST")
                    .message(authorUsername + " created a new post")
                    .relatedPostId(postId)
                    .build();
            notification = notificationRepository.save(notification);
            webSocketHandler.sendToUser(follower.getId(), toDTO(notification));
        }
        log.info("Notified {} followers about post {}", followers.size(), postId);
    }

    @CircuitBreaker(fallbackMethod = "followersFallback")
    public List<UserDTO> getFollowersSafely(Long authorId) {
        return userServiceClient.getFollowers(authorId);
    }

    public List<UserDTO> followersFallback(Long authorId, Throwable t) {
        log.warn("user-service unreachable, skipping notification fan-out for author {}: {}",
                authorId, t.getMessage());
        return Collections.emptyList();
    }

    public void notifyNewFollower(Long followedUserId, String followerUsername) {
        Notification notification = Notification.builder()
                .userId(followedUserId)
                .type("NEW_FOLLOWER")
                .message(followerUsername + " started following you")
                .build();
        notification = notificationRepository.save(notification);
        webSocketHandler.sendToUser(followedUserId, toDTO(notification));
    }

    public List<NotificationDTO> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType())
                .message(n.getMessage())
                .relatedPostId(n.getRelatedPostId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
