package com.online_games_service.authorization;

import org.testng.annotations.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

@SpringBootTest
@ActiveProfiles("test")
public class AuthorizationApplicationTests extends AbstractTestNGSpringContextTests {

    @Test
    public void contextLoads() {
    }

}