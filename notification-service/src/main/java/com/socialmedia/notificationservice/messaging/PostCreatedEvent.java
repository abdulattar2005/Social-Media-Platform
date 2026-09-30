package com.socialmedia.notificationservice.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Mirrors post-service's PostCreatedEvent contract. In a microservices
 * architecture each service owns its own copy of the event schema it
 * consumes (no shared library coupling) - documented in docs/API_CONTRACTS.md.
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
