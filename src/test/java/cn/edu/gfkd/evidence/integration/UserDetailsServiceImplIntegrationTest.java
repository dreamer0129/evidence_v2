package cn.edu.gfkd.evidence.integration;

import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.repository.UserRepository;
import cn.edu.gfkd.evidence.service.UserDetailsServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserDetailsServiceImplIntegrationTest {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        userRepository.save(user);

        // When
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
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
        User user1 = new User();
        user1.setUsername("TestUser");
        user1.setEmail("test1@example.com");
        user1.setPassword("password1");
        user1.setRole("USER");
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("testuser");
        user2.setEmail("test2@example.com");
        user2.setPassword("password2");
        user2.setRole("USER");
        userRepository.save(user2);

        // When
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("password2");
    }

    @Test
    void loadUserByUsername_MultipleRoles_ReturnsCorrectAuthorities() {
        // Given
        User adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("adminPass");
        adminUser.setRole("ADMIN");
        userRepository.save(adminUser);

        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("userPass");
        regularUser.setRole("USER");
        userRepository.save(regularUser);

        // When
        UserDetails adminDetails = userDetailsService.loadUserByUsername("admin");
        UserDetails userDetails = userDetailsService.loadUserByUsername("user");

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
        User user = new User();
        user.setUsername("user@domain.com");
        user.setEmail("user@domain.com");
        user.setPassword("password123");
        user.setRole("USER");
        userRepository.save(user);

        // When
        UserDetails result = userDetailsService.loadUserByUsername("user@domain.com");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user@domain.com");
        assertThat(result.getPassword()).isEqualTo("password123");
    }
}