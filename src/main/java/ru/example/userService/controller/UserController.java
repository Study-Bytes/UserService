package ru.example.userService.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.example.userService.dto.UserDto;
import ru.example.userService.entity.User;
import ru.example.userService.service.UserService;

//контроллер для работы с профилем (/api/users/me)
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    //добавить регулярку на email
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        String email = authentication.getName();
        log.debug("Getting profile for {}", email);
        User user = userService.findByEmail(email);
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @GetMapping("/{id}")
    public UserDto getById(@PathVariable Long id) {
        log.info("Admin request user id = {}", id);
        User user = userService.findById(id);
        return new UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
