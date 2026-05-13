package ru.example.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

//dto для логина

public class LoginRequest {
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
