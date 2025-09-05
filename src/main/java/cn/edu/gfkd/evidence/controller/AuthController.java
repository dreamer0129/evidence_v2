package cn.edu.gfkd.evidence.controller;

import cn.edu.gfkd.evidence.dto.JwtResponse;
import cn.edu.gfkd.evidence.dto.UserLoginDto;
import cn.edu.gfkd.evidence.dto.UserRegistrationDto;
import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.service.user.UserService;
import cn.edu.gfkd.evidence.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtils jwtUtils;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            userService.registerUser(registrationDto);
            return ResponseEntity.ok().body("用户注册成功!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody UserLoginDto loginDto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateTokenFromUsername(loginDto.getUsername());

            User user = (User) authentication.getPrincipal();

            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: 用户名或密码错误");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body("用户未登录");
        }

        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(user);
    }
}