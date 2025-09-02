package cn.edu.gfkd.evidence.util;

import cn.edu.gfkd.evidence.entity.User;
import cn.edu.gfkd.evidence.utils.JwtUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "testSecretKeyThatIsAtLeast256BitsLongForJWT");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
    }

    @Test
    void generateJwtToken_ValidUser_ReturnsToken() {
        String token = jwtUtils.generateJwtToken(testUser);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    void getUsernameFromJwtToken_ValidToken_ReturnsUsername() {
        String token = jwtUtils.generateJwtToken(testUser);

        String username = jwtUtils.getUsernameFromJwtToken(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void validateJwtToken_ValidToken_ReturnsTrue() {
        String token = jwtUtils.generateJwtToken(testUser);

        boolean isValid = jwtUtils.validateJwtToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    void validateJwtToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtils.validateJwtToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateJwtToken_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -1);
        String token = jwtUtils.generateJwtToken(testUser);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean isValid = jwtUtils.validateJwtToken(token);

        assertThat(isValid).isFalse();
    }
}