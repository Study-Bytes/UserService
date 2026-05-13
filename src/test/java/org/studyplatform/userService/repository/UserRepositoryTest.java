package org.studyplatform.userService.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.studyplatform.userService.entity.Role;
import org.studyplatform.userService.entity.User;

import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserByEmail() {
        User user = new User();
        user.setRole(Role.valueOf("STUDENT"));
        user.setEmail("test@example.com");
        user.setPassword("password");
        userRepository.save(user);
        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturnEmptyWhenEmailNotFound() {
        Optional<User> found = userRepository.findByEmail("non-existent@example.com");
        assertThat(found).isEmpty();
    }
}