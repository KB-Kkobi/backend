package org.kkobi.security.util;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RootConfig;
import org.kkobi.security.config.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(SpringExtension.class)
@Log4j2
@ContextConfiguration(classes = {RootConfig.class, SecurityConfig.class})
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
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMCIsImlhdCI6MTc4MjcwMDEzOCwiZXhwIjoxNzgyNzAwNDM4fQ._p2zBf3WKDFMCMJbFe9DoLWS5oZlk94Pg4ohOrrL70kcffkF_-nwjTdW4xX1mFCQ";
        String username = jwtProcessor.getUsername(token);
        log.info(username);
        assertNotNull(username);
    }

    @Test
    void validateToken(){
        String token = "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ1c2VyMCIsImlhdCI6MTc4MjcwMDEzOCwiZXhwIjoxNzgyNzAwNDM4fQ._p2zBf3WKDFMCMJbFe9DoLWS5oZlk94Pg4ohOrrL70kcffkF_-nwjTdW4xX1mFCQ";

        boolean isValid = jwtProcessor.validateToken(token);
        log.info(isValid);
        assertTrue(isValid);
    }

}