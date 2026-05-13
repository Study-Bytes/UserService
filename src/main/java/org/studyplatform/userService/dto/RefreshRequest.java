package ru.example.userService.dto;

import jakarta.validation.constraints.NotBlank;

//dto для обмена refresh-token на новый access-token

public class RefreshRequest {
    @NotBlank
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
