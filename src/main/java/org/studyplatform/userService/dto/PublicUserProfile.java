package org.studyplatform.userService.dto;

import org.studyplatform.userService.entity.User;

public class PublicUserProfile {
    private Long id;
    private String fullName;
    private String avatarUrl;

    public PublicUserProfile(Long id, String fullName, String avatarUrl) {
        this.id = id;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
    }

    public static PublicUserProfile from(User user) {
        return new PublicUserProfile(
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl()
        );
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
