package org.studyplatform.userService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserSettingsRequest {
    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 1000)
    private String avatarUrl;

    @Size(max = 2000)
    private String bio;

    @NotBlank
    @Pattern(regexp = "ru|en", message = "Unsupported locale")
    private String preferredLocale;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getPreferredLocale() { return preferredLocale; }
    public void setPreferredLocale(String preferredLocale) { this.preferredLocale = preferredLocale; }
}
