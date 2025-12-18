package com.online_games_service.test.containers;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.MongoDBContainer;

public class MongoContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4.4");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        if (!MONGO.isRunning()) {
            MONGO.start();
        }
        
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
            "spring.data.mongodb.uri=" + MONGO.getReplicaSetUrl()
        );
    }
}