package cn.edu.gfkd.evidence.repository;

import cn.edu.gfkd.evidence.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private String generateUniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateUniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByUsername(username);

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo(username);
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail(email);

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(email);
    }

    @Test
    void existsByUsername_ExistingUser_ReturnsTrue() {
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByUsername(username);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_NonExistingUser_ReturnsFalse() {
        boolean exists = userRepository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_ExistingUser_ReturnsTrue() {
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        entityManager.persistAndFlush(user);

        boolean exists = userRepository.existsByEmail(email);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_NonExistingUser_ReturnsFalse() {
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }
}