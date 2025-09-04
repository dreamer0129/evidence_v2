package cn.edu.gfkd.evidence.integration;

import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.repository.UserRepository;
import cn.edu.gfkd.evidence.service.UserDetailsServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserDetailsServiceImplIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserRepository userRepository;

    private String generateUniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateUniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
        // Given
        String username = generateUniqueUsername();
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setRole("USER");
        userRepository.save(user);

        // When
        UserDetails result = userDetailsService.loadUserByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsername_NonExistingUser_ThrowsUsernameNotFoundException() {
        // When & Then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: nonexistent");
    }

    @Test
    void loadUserByUsername_CaseSensitive_ReturnsCorrectUser() {
        // Given
        String username1 = "TestUser_" + UUID.randomUUID().toString().substring(0, 4);
        String username2 = "testuser_" + UUID.randomUUID().toString().substring(0, 4);
        
        User user1 = new User();
        user1.setUsername(username1);
        user1.setEmail(generateUniqueEmail());
        user1.setPassword("password1");
        user1.setRole("USER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername(username2);
        user2.setEmail(generateUniqueEmail());
        user2.setPassword("password2");
        user2.setRole("USER");
        userRepository.save(user2);

        // When
        UserDetails result = userDetailsService.loadUserByUsername(username2);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username2);
        assertThat(result.getPassword()).isEqualTo("password2");
    }

    @Test
    void loadUserByUsername_MultipleRoles_ReturnsCorrectAuthorities() {
        // Given
        String adminUsername = "admin_" + UUID.randomUUID().toString().substring(0, 4);
        String regularUsername = "user_" + UUID.randomUUID().toString().substring(0, 4);
        
        User adminUser = new User();
        adminUser.setUsername(adminUsername);
        adminUser.setEmail(generateUniqueEmail());
        adminUser.setPassword("adminPass");
        adminUser.setRole("ADMIN");
        userRepository.save(adminUser);

        User regularUser = new User();
        regularUser.setUsername(regularUsername);
        regularUser.setEmail(generateUniqueEmail());
        regularUser.setPassword("userPass");
        regularUser.setRole("USER");
        userRepository.save(regularUser);

        // When
        UserDetails adminDetails = userDetailsService.loadUserByUsername(adminUsername);
        UserDetails userDetails = userDetailsService.loadUserByUsername(regularUsername);

        // Then
        assertThat(adminDetails.getAuthorities()).hasSize(1);
        assertThat(adminDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");

        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsername_WithSpecialCharacters_ReturnsUserDetails() {
        // Given
        String username = "user_" + UUID.randomUUID().toString().substring(0, 4) + "@domain.com";
        String email = generateUniqueEmail();
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole("USER");
        userRepository.save(user);

        // When
        UserDetails result = userDetailsService.loadUserByUsername(username);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo("password123");
    }
}