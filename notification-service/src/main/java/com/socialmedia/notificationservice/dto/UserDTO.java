package com.socialmedia.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the shape returned by user-service; used only for Feign deserialization. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String avatarUrl;
    private long followerCount;
    private long followingCount;
}
