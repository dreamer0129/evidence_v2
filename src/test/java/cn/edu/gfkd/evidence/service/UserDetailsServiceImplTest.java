package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Unit Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole("USER");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should return UserDetails when user exists")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("ROLE_USER", authority.getAuthority());
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user does not exist")
    void loadUserByUsername_WhenUserDoesNotExist_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("nonexistent")
        );

        assertEquals("User not found: nonexistent", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should return UserDetails with ADMIN role when user has ADMIN role")
    void loadUserByUsername_WhenUserHasAdminRole_ShouldReturnAdminUserDetails() {
        // Arrange
        testUser.setRole("ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("admin");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(1, authorities.size());
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("ROLE_ADMIN", authority.getAuthority());
        
        // Verify interactions
        verify(userRepository).findByUsername("admin");
    }

    @DisplayName("Should throw UsernameNotFoundException when username is null")
    @Test
    void loadUserByUsername_WhenUsernameIsNull_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(null)
        );

        assertEquals("User not found: null", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername(null);
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when username is empty")
    void loadUserByUsername_WhenUsernameIsEmpty_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("")
        );

        assertEquals("User not found: ", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername("");
    }

    @Test
    @DisplayName("Should handle repository exception and propagate it")
    void loadUserByUsername_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.findByUsername("testuser"))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userDetailsService.loadUserByUsername("testuser")
        );

        assertEquals("Database connection failed", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return UserDetails with correct user account properties")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetailsWithCorrectAccountProperties() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertTrue(result.isAccountNonExpired(), "Account should be non-expired");
        assertTrue(result.isAccountNonLocked(), "Account should be non-locked");
        assertTrue(result.isCredentialsNonExpired(), "Credentials should be non-expired");
        assertTrue(result.isEnabled(), "Account should be enabled");
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return UserDetails with username matching input")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetailsWithMatchingUsername() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername(), "Username should match input");
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return UserDetails with password from database")
    void loadUserByUsername_WhenUserExists_ShouldReturnUserDetailsWithDatabasePassword() {
        // Arrange
        testUser.setPassword("databaseEncodedPassword");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("databaseEncodedPassword", result.getPassword(), "Password should match database password");
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return UserDetails with authorities based on user role")
    void loadUserByUsername_WhenUserHasCustomRole_ShouldReturnUserDetailsWithCorrectAuthority() {
        // Arrange
        testUser.setRole("MODERATOR");
        when(userRepository.findByUsername("moderator")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("moderator");

        // Assert
        assertNotNull(result);
        
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertEquals(1, authorities.size());
        
        GrantedAuthority authority = authorities.iterator().next();
        assertEquals("ROLE_MODERATOR", authority.getAuthority());
        
        // Verify interactions
        verify(userRepository).findByUsername("moderator");
    }

    @Test
    @DisplayName("Should be case sensitive for username lookup")
    void loadUserByUsername_WhenUsernameCaseDifferent_ShouldBeCaseSensitive() {
        // Arrange
        when(userRepository.findByUsername("TestUser")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("TestUser")
        );

        assertEquals("User not found: TestUser", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername("TestUser");
        verify(userRepository, never()).findByUsername("testuser");
    }
}