package com.online_games_service.test.containers;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class RedisContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withCommand("redis-server", "--requirepass", "testredispass", "--notify-keyspace-events", "Ex");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }

        Integer mappedPort = REDIS.getMappedPort(6379);
        String host = REDIS.getHost();

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                "spring.data.redis.host=" + host,
                "spring.data.redis.port=" + mappedPort,
                "spring.data.redis.password=testredispass"
        );
    }
}