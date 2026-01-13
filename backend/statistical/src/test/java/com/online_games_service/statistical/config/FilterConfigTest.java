package com.online_games_service.statistical.config;

import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for FilterConfig.
 */
public class FilterConfigTest {

    private FilterConfig filterConfig;

    @BeforeMethod
    public void setUp() {
        filterConfig = new FilterConfig();
    }

    @Test
    public void sessionUserFilterRegistration_registersFilter() {
        SessionUserFilter filter = mock(SessionUserFilter.class);

        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(filter);

        assertNotNull(registration);
        assertEquals(registration.getFilter(), filter);
    }

    @Test
    public void sessionUserFilterRegistration_setsCorrectUrlPatterns() {
        SessionUserFilter filter = mock(SessionUserFilter.class);

        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(filter);

        Collection<String> patterns = registration.getUrlPatterns();
        assertNotNull(patterns);
        assertTrue(patterns.contains("/me"));
        assertTrue(patterns.contains("/me/*"));
    }

    @Test
    public void sessionUserFilterRegistration_setsOrder() {
        SessionUserFilter filter = mock(SessionUserFilter.class);

        FilterRegistrationBean<SessionUserFilter> registration = 
                filterConfig.sessionUserFilterRegistration(filter);

        assertEquals(registration.getOrder(), 1);
    }
}
