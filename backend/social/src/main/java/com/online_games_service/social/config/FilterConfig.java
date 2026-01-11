package com.online_games_service.social.config;

import com.online_games_service.common.config.SessionReaderConfig;
import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration for registering the SessionUserFilter on protected endpoints.
 * 
 * All REST endpoints require authentication:
 * - /friends/* - Friend request operations
 * - /presence/* - Presence status operations
 */
@Configuration
@Import(SessionReaderConfig.class)
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SessionUserFilter> sessionUserFilterRegistration(SessionUserFilter filter) {
        FilterRegistrationBean<SessionUserFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}
