package com.online_games_service.social.config;

import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for FilterConfig.
 * Verifies that SessionUserFilter is registered for all API endpoints.
 */
public class FilterConfigTest {

    private FilterConfig filterConfig;
    private SessionUserFilter mockFilter;

    @BeforeMethod
    public void setUp() {
        filterConfig = new FilterConfig();
        mockFilter = mock(SessionUserFilter.class);
    }

    @Test
    public void sessionUserFilterRegistration_ReturnsNonNullBean() {
        // When
        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(mockFilter);

        // Then
        Assert.assertNotNull(registration);
    }

    @Test
    public void sessionUserFilterRegistration_HasCorrectFilter() {
        // When
        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(mockFilter);

        // Then
        Assert.assertEquals(registration.getFilter(), mockFilter);
    }

    @Test
    public void sessionUserFilterRegistration_RegistersAllApiEndpoints() {
        // When
        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(mockFilter);

        // Then
        Collection<String> urlPatterns = registration.getUrlPatterns();
        Assert.assertNotNull(urlPatterns);
        Assert.assertTrue(urlPatterns.contains("/*"), 
                "Should register /* pattern to cover all endpoints");
    }

    @Test
    public void sessionUserFilterRegistration_HasCorrectOrder() {
        // When
        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(mockFilter);

        // Then
        Assert.assertEquals(registration.getOrder(), 1);
    }

    @Test
    public void sessionUserFilterRegistration_UsesWildcardPattern() {
        // When
        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(mockFilter);

        // Then - single wildcard pattern covers all API endpoints
        Collection<String> urlPatterns = registration.getUrlPatterns();
        Assert.assertEquals(urlPatterns.size(), 1);
        Assert.assertTrue(urlPatterns.iterator().next().endsWith("/*"),
                "Should use wildcard pattern for all API endpoints");
    }
}
