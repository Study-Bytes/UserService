package org.studyplatform.userService.dto;

import org.studyplatform.userService.entity.User;

public class UserSettingsResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String avatarUrl;
    private String bio;
    private String preferredLocale;

    public UserSettingsResponse(Long id, String email, String fullName, String role, String status,
                                String avatarUrl, String bio, String preferredLocale) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.preferredLocale = preferredLocale;
    }

    public static UserSettingsResponse from(User user) {
        return new UserSettingsResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getPreferredLocale()
        );
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public String getPreferredLocale() { return preferredLocale; }
}
