package org.studyplatform.userService.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.studyplatform.userService.dto.ChangePasswordRequest;
import org.studyplatform.userService.dto.CurrentUser;
import org.studyplatform.userService.dto.UpdateProfileRequest;
import org.studyplatform.userService.entity.User;
import org.studyplatform.userService.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return CurrentUser.from(user);
    }

    @PutMapping("/me/profile")
    public CurrentUser updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        User user = userService.updateProfile(authentication.getName(), request);
        return CurrentUser.from(user);
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CurrentUser getById(@PathVariable Long id) {
        User user = userService.findById(id);
        return CurrentUser.from(user);
    }
}
