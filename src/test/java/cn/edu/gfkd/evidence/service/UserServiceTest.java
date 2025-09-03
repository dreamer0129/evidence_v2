package cn.edu.gfkd.evidence.service;

import cn.edu.gfkd.evidence.dto.UserRegistrationDto;
import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.exception.UserNotFoundException;
import cn.edu.gfkd.evidence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDto validRegistrationDto;
    private User savedUser;

    @BeforeEach
    void setUp() {
        validRegistrationDto = new UserRegistrationDto();
        validRegistrationDto.setUsername("testuser");
        validRegistrationDto.setEmail("test@example.com");
        validRegistrationDto.setPassword("password123");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole("USER");
        savedUser.setCreatedAt(LocalDateTime.now());
        savedUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should register user successfully when data is valid")
    void registerUser_WhenValidData_ShouldRegisterUser() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.registerUser(validRegistrationDto);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("encodedPassword", result.getPassword());
        assertEquals("USER", result.getRole());

        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void registerUser_WhenUsernameExists_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Username already exists: testuser", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void registerUser_WhenEmailExists_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Email already exists: test@example.com", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when registration data is null")
    void registerUser_WhenDataIsNull_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(null)
        );

        assertEquals("Registration data cannot be null", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw exception when username is empty")
    void registerUser_WhenUsernameIsEmpty_ShouldThrowException() {
        // Arrange
        validRegistrationDto.setUsername("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Username cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw exception when email is empty")
    void registerUser_WhenEmailIsEmpty_ShouldThrowException() {
        // Arrange
        validRegistrationDto.setEmail("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Email cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should throw exception when password is empty")
    void registerUser_WhenPasswordIsEmpty_ShouldThrowException() {
        // Arrange
        validRegistrationDto.setPassword("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Password cannot be empty", exception.getMessage());
        
        // Verify no interactions
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Should return user when username exists")
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(savedUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should return empty when username does not exist")
    void findByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
        
        // Verify interactions
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("Should return user when email exists")
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(savedUser));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        
        // Verify interactions
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("Should return empty when email does not exist")
    void findByEmail_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
        
        // Verify interactions
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    @DisplayName("Should handle repository exception during user registration")
    void registerUser_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Database error", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle password encoder exception during user registration")
    void registerUser_WhenPasswordEncoderThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenThrow(new RuntimeException("Encoding error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.registerUser(validRegistrationDto)
        );

        assertEquals("Encoding error", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).existsByUsername("testuser");
        verify(userRepository).existsByEmail("test@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle repository exception during username lookup")
    void findByUsername_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.findByUsername("testuser")
        );

        assertEquals("Database error", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("Should handle repository exception during email lookup")
    void findByEmail_WhenRepositoryThrowsException_ShouldPropagateException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> userService.findByEmail("test@example.com")
        );

        assertEquals("Database error", exception.getMessage());
        
        // Verify interactions
        verify(userRepository).findByEmail("test@example.com");
    }
}