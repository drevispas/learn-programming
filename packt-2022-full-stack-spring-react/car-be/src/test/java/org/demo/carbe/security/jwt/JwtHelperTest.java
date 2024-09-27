package org.demo.carbe.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtHelperTest {

    @Autowired
    private JwtHelper jwtHelper;

    private final String subject = "john";
    private final Map<String, Object> claims = Map.of("role", "admin");

    private final Logger log = Logger.getLogger(JwtHelperTest.class.getName());

    @Test
    void createToken() {
        String token = jwtHelper.createToken(claims, subject);
        assertNotNull(token);
        log.info("Token: " + token);
    }

    @Test
    void validateToken() {
        String token = jwtHelper.createToken(claims, subject);
        UserDetails userDetails = User.withUsername(subject)
                .password("user")
                .authorities("read", "write")
                .build();
        assertTrue(jwtHelper.validateToken(token, userDetails));
    }
}
