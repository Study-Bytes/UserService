package org.studyplatform.userService.dto;

import org.studyplatform.userService.entity.User;

public class CurrentUser {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
    private String avatarUrl;
    private String bio;

    public CurrentUser(Long id, String email, String fullName, String role, String status, String avatarUrl, String bio) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public static CurrentUser from(User user) {
        return new CurrentUser(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getAvatarUrl(),
                user.getBio()
        );
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }
}
