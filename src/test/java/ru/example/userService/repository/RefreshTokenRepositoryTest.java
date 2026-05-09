package ru.example.userService.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.example.userService.entity.RefreshToken;
import ru.example.userService.entity.Role;
import ru.example.userService.entity.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setRole(Role.valueOf("STUDENT"));
        testUser.setEmail("user@example.com");
        testUser.setPassword("pass");
        userRepository.save(testUser);
    }

    @Test
    void shouldFindByTokenHash() {
        String hash = "some_secure_hash";
        RefreshToken token = new RefreshToken(hash, testUser, Instant.now().plus(1, ChronoUnit.DAYS));
        entityManager.persist(token);
        entityManager.flush();
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(hash);
        assertThat(found).isPresent();
        assertThat(found.get().getTokenHash()).isEqualTo(hash);
        assertThat(found.get().getUser().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void shouldRevokeAllActiveTokensByUser() {
        RefreshToken active1 = new RefreshToken("hash1", testUser, Instant.now().plus(1, ChronoUnit.HOURS));
        RefreshToken active2 = new RefreshToken("hash2", testUser, Instant.now().plus(1, ChronoUnit.HOURS));
        RefreshToken alreadyRevoked = new RefreshToken("hash3", testUser, Instant.now().plus(1, ChronoUnit.HOURS));
        alreadyRevoked.setRevoked(true);
        entityManager.persist(active1);
        entityManager.persist(active2);
        entityManager.persist(alreadyRevoked);
        entityManager.flush();
        refreshTokenRepository.revokeAllByUser(testUser);
        entityManager.clear();
        RefreshToken updated1 = entityManager.find(RefreshToken.class, active1.getId());
        RefreshToken updated2 = entityManager.find(RefreshToken.class, active2.getId());
        RefreshToken updated3 = entityManager.find(RefreshToken.class, alreadyRevoked.getId());

        assertThat(updated1.isRevoked()).isTrue();
        assertThat(updated2.isRevoked()).isTrue();
        assertThat(updated3.isRevoked()).isTrue(); // Остался true
    }

    private RefreshToken createToken(String hash, boolean revoked) {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash);
        token.setRevoked(revoked);
        token.setUser(testUser);
        return token;
    }
}
