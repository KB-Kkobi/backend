package org.kkobi.security.util;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.security.config.JwtConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@Log4j2
@ContextConfiguration(classes = {JwtConfig.class, JwtProcessor.class})
@TestPropertySource(properties = {
        "jwt.secret=test-jwt-secret-key-at-least-32-bytes-long",
        "jwt.issuer=kkobi-test",
        "jwt.access-token-validity-ms=1800000",
        "jwt.refresh-token-validity-ms=1209600000"
})
class JwtProcessorTest {
    @Autowired
    JwtProcessor jwtProcessor;

    @Test
    void generateToken(){
        String username = "user0";
        String token = jwtProcessor.generateToken(username);
        log.info(token);
        assertNotNull(token);
    }

    @Test
    void getUsername(){
        String token = jwtProcessor.generateToken("user0");
        String username = jwtProcessor.getUsername(token);
        log.info(username);
        assertEquals("user0", username);
    }

    @Test
    void validateToken(){
        String token = jwtProcessor.generateToken("user0");

        boolean isValid = jwtProcessor.validateToken(token);
        log.info(isValid);
        assertTrue(isValid);
    }

}
