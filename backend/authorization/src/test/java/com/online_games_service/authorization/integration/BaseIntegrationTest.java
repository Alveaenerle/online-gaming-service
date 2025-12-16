package com.online_games_service.authorization.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest extends AbstractTestNGSpringContextTests {

    static final MongoDBContainer mongoDBContainer;
    static final GenericContainer<?> redisContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"));
        mongoDBContainer.start();

        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379)
                .withCommand("redis-server --requirepass testredispass");
        redisContainer.start();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        // Mongo URI
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        
        // Redis Host/Port/Pass
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
        registry.add("spring.data.redis.password", () -> "testredispass");
    }
}