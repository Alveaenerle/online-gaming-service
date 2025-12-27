package com.online_games_service.makao.config;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MakaoRedisConfigTest {

    @Test
    public void shouldBeAnnotatedWithConfiguration() {
        Assert.assertTrue(MakaoRedisConfig.class.isAnnotationPresent(org.springframework.context.annotation.Configuration.class));
    }

    @Test
    public void shouldImportBaseRedisConfig() {
        Assert.assertTrue(MakaoRedisConfig.class.isAnnotationPresent(org.springframework.context.annotation.Import.class));
    }

    @Test
    public void shouldEnableRedisRepositories() {
        Assert.assertTrue(MakaoRedisConfig.class.isAnnotationPresent(org.springframework.data.redis.repository.configuration.EnableRedisRepositories.class));
    }
    
    @Test
    public void shouldInstantiate() {
        MakaoRedisConfig config = new MakaoRedisConfig();
        Assert.assertNotNull(config);
    }
}
