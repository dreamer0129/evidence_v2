package cn.edu.gfkd.evidence.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import cn.edu.gfkd.evidence.dto.UserRegistrationDto;
import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.repository.UserRepository;
import cn.edu.gfkd.evidence.service.UserService;

@SpringBootTest @ActiveProfiles("test") @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String generateUniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateUniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void registerUser_ValidData_CreatesUserSuccessfully() {
        // Given
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername(username);
        registrationDto.setEmail(email);
        registrationDto.setPassword("password123");

        // When
        User savedUser = userService.registerUser(registrationDto);

        // Then
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo(username);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getRole()).isEqualTo("USER");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword())).isTrue();

        // Verify user is persisted in database
        Optional<User> foundUser = userRepository.findByUsername(username);
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
    }

    @Test
    void registerUser_DuplicateUsername_ThrowsException() {
        // Given
        UserRegistrationDto firstDto = new UserRegistrationDto();
        firstDto.setUsername("duplicateuser");
        firstDto.setEmail("first@example.com");
        firstDto.setPassword("password123");

        UserRegistrationDto secondDto = new UserRegistrationDto();
        secondDto.setUsername("duplicateuser");
        secondDto.setEmail("second@example.com");
        secondDto.setPassword("password456");

        // When
        userService.registerUser(firstDto);

        // Then
        assertThatThrownBy(() -> userService.registerUser(secondDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        // Given
        String duplicateEmail = generateUniqueEmail();

        UserRegistrationDto firstDto = new UserRegistrationDto();
        firstDto.setUsername("firstuser");
        firstDto.setEmail(duplicateEmail);
        firstDto.setPassword("password123");

        UserRegistrationDto secondDto = new UserRegistrationDto();
        secondDto.setUsername("seconduser");
        secondDto.setEmail(duplicateEmail);
        secondDto.setPassword("password456");

        // When
        userService.registerUser(firstDto);

        // Then
        assertThatThrownBy(() -> userService.registerUser(secondDto))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("Email already exists");
    }

    @Test
    void registerUser_NullData_ThrowsException() {
        // When & Then
        assertThatThrownBy(() -> userService.registerUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Registration data cannot be null");
    }

    @Test
    void registerUser_EmptyUsername_ThrowsException() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("");
        registrationDto.setEmail(generateUniqueEmail());
        registrationDto.setPassword("password123");

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void registerUser_EmptyEmail_ThrowsException() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername(generateUniqueUsername());
        registrationDto.setEmail("");
        registrationDto.setPassword("password123");

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be empty");
    }

    @Test
    void registerUser_EmptyPassword_ThrowsException() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername(generateUniqueUsername());
        registrationDto.setEmail(generateUniqueEmail());
        registrationDto.setPassword("");

        // When & Then
        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be empty");
    }

    @Test
    void findByUsername_ExistingUser_ReturnsUser() {
        // Given
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername(username);
        registrationDto.setEmail(email);
        registrationDto.setPassword("password123");

        User savedUser = userService.registerUser(registrationDto);

        // When
        Optional<User> foundUser = userService.findByUsername(username);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getEmail()).isEqualTo(email);
    }

    @Test
    void findByUsername_NonExistingUser_ReturnsEmpty() {
        // When
        Optional<User> foundUser = userService.findByUsername("nonexisting");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void findByEmail_ExistingUser_ReturnsUser() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("emailuser");
        registrationDto.setEmail("emailuser@example.com");
        registrationDto.setPassword("password123");

        userService.registerUser(registrationDto);

        // When
        Optional<User> foundUser = userService.findByEmail("emailuser@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("emailuser");
    }

    @Test
    void findByEmail_NonExistingUser_ReturnsEmpty() {
        // When
        Optional<User> foundUser = userService.findByEmail("nonexisting@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }

    @Test
    void registerUser_MultipleUsers_CreatesAllWithUniqueIds() {
        // Given
        UserRegistrationDto dto1 = new UserRegistrationDto();
        dto1.setUsername(generateUniqueUsername());
        dto1.setEmail(generateUniqueEmail());
        dto1.setPassword("password123");

        UserRegistrationDto dto2 = new UserRegistrationDto();
        dto2.setUsername(generateUniqueUsername());
        dto2.setEmail(generateUniqueEmail());
        dto2.setPassword("password456");

        UserRegistrationDto dto3 = new UserRegistrationDto();
        dto3.setUsername("user3");
        dto3.setEmail("user3@example.com");
        dto3.setPassword("password789");

        // When
        User user1 = userService.registerUser(dto1);
        User user2 = userService.registerUser(dto2);
        User user3 = userService.registerUser(dto3);

        // Then
        assertThat(user1.getId()).isNotNull();
        assertThat(user2.getId()).isNotNull();
        assertThat(user3.getId()).isNotNull();
        assertThat(user1.getId()).isNotEqualTo(user2.getId());
        assertThat(user2.getId()).isNotEqualTo(user3.getId());
        assertThat(user1.getId()).isNotEqualTo(user3.getId());

        // Verify all users are in database
        assertThat(userRepository.count()).isEqualTo(3);
    }

    @Test
    void findByUsername_CaseSensitive_ReturnsCorrectUser() {
        // Given
        String username1 = generateUniqueUsername();
        String username2 = generateUniqueUsername();

        UserRegistrationDto dto1 = new UserRegistrationDto();
        dto1.setUsername(username1);
        dto1.setEmail("test1@example.com");
        dto1.setPassword("password123");

        UserRegistrationDto dto2 = new UserRegistrationDto();
        dto2.setUsername(username2);
        dto2.setEmail("test2@example.com");
        dto2.setPassword("password456");

        userService.registerUser(dto1);
        userService.registerUser(dto2);

        // When
        Optional<User> foundUser = userService.findByUsername(username2);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test2@example.com");
    }

    @Test
    void findByEmail_CaseSensitive_ReturnsCorrectUser() {
        // Given
        String email1 = "Test1@example.com";
        String email2 = "Test2@example.com";

        UserRegistrationDto dto1 = new UserRegistrationDto();
        dto1.setUsername(generateUniqueUsername());
        dto1.setEmail(email1);
        dto1.setPassword("password123");

        UserRegistrationDto dto2 = new UserRegistrationDto();
        dto2.setUsername(generateUniqueUsername());
        dto2.setEmail(email2);
        dto2.setPassword("password456");

        userService.registerUser(dto1);
        userService.registerUser(dto2);

        // When
        Optional<User> foundUser = userService.findByEmail(email2);

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isNotEqualTo(dto1.getUsername());
    }

    @Test
    void registerUser_PasswordIsEncoded_PasswordNotStoredInPlainText() {
        // Given
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("secureuser");
        registrationDto.setEmail("secure@example.com");
        registrationDto.setPassword("mypassword123");

        // When
        User savedUser = userService.registerUser(registrationDto);

        // Then
        assertThat(savedUser.getPassword()).isNotEqualTo("mypassword123");
        assertThat(savedUser.getPassword()).doesNotContain("mypassword");
        assertThat(passwordEncoder.matches("mypassword123", savedUser.getPassword())).isTrue();
    }
}