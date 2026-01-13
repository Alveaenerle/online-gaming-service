package com.online_games_service.statistical.config;

import com.online_games_service.common.config.SessionReaderConfig;
import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for registering the SessionUserFilter on protected endpoints.
 */
@Configuration
@Import(SessionReaderConfig.class)
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SessionUserFilter> sessionUserFilterRegistration(SessionUserFilter filter) {
        FilterRegistrationBean<SessionUserFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        // Apply filter to /me endpoints which require authentication
        registrationBean.addUrlPatterns("/me", "/me/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
