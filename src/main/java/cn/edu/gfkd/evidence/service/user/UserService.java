package cn.edu.gfkd.evidence.service.user;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.edu.gfkd.evidence.dto.UserRegistrationDto;
import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.exception.UserNotFoundException;
import cn.edu.gfkd.evidence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service @RequiredArgsConstructor @Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(UserRegistrationDto registrationDto) {
        validateRegistrationDto(registrationDto);

        log.info("Registering new user: {}", registrationDto.getUsername());

        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserNotFoundException(
                    "Username already exists: " + registrationDto.getUsername());
        }

        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserNotFoundException("Email already exists: " + registrationDto.getEmail());
        }

        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole("USER");

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {}", savedUser.getId());
        return savedUser;
    }

    private void validateRegistrationDto(UserRegistrationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Registration data cannot be null");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}