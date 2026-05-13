package ru.example.userService.dto;

//ответ при успешной аутенфикации

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long usedId;
    private String email;
    private String role;

    public AuthResponse(String accessToken, String refreshToken, Long usedId, String email, String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.usedId = usedId;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    public Long getUsedId() {
        return usedId;
    }
    public void setUsedId(Long usedId) {
        this.usedId = usedId;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
}
