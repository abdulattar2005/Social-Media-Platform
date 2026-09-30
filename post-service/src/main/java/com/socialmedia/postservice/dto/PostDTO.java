package com.socialmedia.postservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private Long id;
    private Long authorId;
    private String authorUsername;
    private String content;
    private String mediaUrl;
    private int likeCount;
    private boolean likedByCurrentUser;
    private LocalDateTime createdAt;
}
