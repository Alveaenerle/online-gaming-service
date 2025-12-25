package com.online_games_service.menu.config;

import com.online_games_service.common.config.SessionReaderConfig;
import com.online_games_service.common.filter.SessionUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SessionReaderConfig.class)
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<SessionUserFilter> sessionUserFilterRegistration(SessionUserFilter filter) {
        FilterRegistrationBean<SessionUserFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(filter);

        registrationBean.addUrlPatterns("/create", "/join", "/start", "/leave",
                "/api/menu/*");

        registrationBean.setOrder(1);

        return registrationBean;
    }
}