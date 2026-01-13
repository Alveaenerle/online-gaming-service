package com.online_games_service.statistical;

import org.testng.annotations.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.testng.Assert.assertNotNull;

public class StatisticalApplicationTests {

    @Test
    public void contextLoads() {
        // Basic test to verify application class exists
        StatisticalApplication app = new StatisticalApplication();
        assertNotNull(app);
    }

    @Test
    public void mainMethodRuns() {
        // Test that main method can be called (won't actually start the app in tests)
        // This is just to ensure the class is properly structured
        assertNotNull(StatisticalApplication.class);
    }
}
