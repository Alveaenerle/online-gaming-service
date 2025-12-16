package com.online_games_service.authorization.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = BaseIntegrationTest.DockerInitializer.class) 
public abstract class BaseIntegrationTest extends AbstractTestNGSpringContextTests {

    static final MongoDBContainer mongoDBContainer;
    static final GenericContainer<?> redisContainer;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"));
        mongoDBContainer.start();

        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379)
                .withCommand("redis-server --requirepass testredispass");
        redisContainer.start();
    }

    static class DockerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            String mongoUri = mongoDBContainer.getReplicaSetUrl();
            Integer redisPort = redisContainer.getFirstMappedPort();
            String redisHost = redisContainer.getHost();

            TestPropertyValues.of(
                "spring.data.mongodb.uri=" + mongoUri,
                "spring.data.redis.host=" + redisHost,
                "spring.data.redis.port=" + redisPort,
                "spring.data.redis.password=testredispass" 
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}