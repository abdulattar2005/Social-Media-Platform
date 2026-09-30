package com.socialmedia.postservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published to the "social-media.exchange" with routing key
 * "post.created" whenever a new post is created. notification-service
 * consumes this to fan out alerts to the author's followers, and
 * analytics-service consumes it to update platform statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreatedEvent implements Serializable {
    private Long postId;
    private Long authorId;
    private String authorUsername;
    private String contentPreview;
    private LocalDateTime createdAt;
}
