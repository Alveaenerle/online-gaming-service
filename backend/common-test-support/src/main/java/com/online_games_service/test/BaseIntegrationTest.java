package com.online_games_service.test;

import com.online_games_service.test.containers.MongoContainerInitializer;
import com.online_games_service.test.containers.RedisContainerInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.rabbitmq.listener.direct.auto-startup=false"
})
@ContextConfiguration(initializers = {
    MongoContainerInitializer.class, 
    RedisContainerInitializer.class
})
public abstract class BaseIntegrationTest extends AbstractTestNGSpringContextTests {
}