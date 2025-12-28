package com.online_games_service.authorization.config;

import com.online_games_service.authorization.security.AuthTokenFilter;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SecurityConfigTest {

    @Mock
    private AuthTokenFilter authTokenFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private HttpSecurity httpSecurity;

    private SecurityConfig securityConfig;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        securityConfig = new SecurityConfig(authTokenFilter);
    }

    @Test
    public void testAnnotations() {
        Assert.assertTrue(SecurityConfig.class.isAnnotationPresent(Configuration.class));
    }

    @Test
    public void testPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        Assert.assertNotNull(encoder);
    }

    @Test
    public void testAuthenticationManager() throws Exception {
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(manager);

        AuthenticationManager result = securityConfig.authenticationManager(authenticationConfiguration);
        Assert.assertEquals(result, manager);
    }

    @Test
    public void testFilterChain() throws Exception {
        // Mocks for configurers
        CsrfConfigurer<HttpSecurity> csrfConfigurer = mock(CsrfConfigurer.class);
        SessionManagementConfigurer<HttpSecurity> sessionConfigurer = mock(SessionManagementConfigurer.class);
        LogoutConfigurer<HttpSecurity> logoutConfigurer = mock(LogoutConfigurer.class);
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authRegistry = mock(AuthorizeHttpRequestsConfigurer.AuthorizationManagerRequestMatcherRegistry.class);
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl = mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        // Mocking HttpSecurity methods to capture Customizers
        when(httpSecurity.csrf(any())).thenAnswer(invocation -> {
            Customizer<CsrfConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(csrfConfigurer);
            return httpSecurity;
        });

        when(httpSecurity.sessionManagement(any())).thenAnswer(invocation -> {
            Customizer<SessionManagementConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(sessionConfigurer);
            return httpSecurity;
        });

        when(httpSecurity.logout(any())).thenAnswer(invocation -> {
            Customizer<LogoutConfigurer<HttpSecurity>> customizer = invocation.getArgument(0);
            customizer.customize(logoutConfigurer);
            return httpSecurity;
        });
        
        when(httpSecurity.authorizeHttpRequests(any())).thenAnswer(invocation -> {
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> customizer = invocation.getArgument(0);
            customizer.customize(authRegistry);
            return httpSecurity;
        });

        // Mocking registry behavior
        when(authRegistry.requestMatchers(any(String[].class))).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(authRegistry);
        when(authRegistry.anyRequest()).thenReturn(authorizedUrl);
        when(authorizedUrl.authenticated()).thenReturn(authRegistry);

        when(httpSecurity.cors(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);
        when(httpSecurity.build()).thenReturn(chain);

        SecurityFilterChain result = securityConfig.filterChain(httpSecurity);
        Assert.assertEquals(result, chain);
        
        // Verifications
        verify(csrfConfigurer).disable();
        verify(sessionConfigurer).sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        verify(logoutConfigurer).disable();
        
        verify(authRegistry).requestMatchers("/register", "/login", "/guest", "/logout");
        verify(authorizedUrl, times(1)).permitAll();
        verify(authRegistry).anyRequest();
        verify(authorizedUrl, times(1)).authenticated();
        
        verify(httpSecurity).addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Test
    public void testCorsConfigurationSource() {
        org.springframework.web.cors.CorsConfigurationSource source = securityConfig.corsConfigurationSource("http://localhost:3000");
        Assert.assertNotNull(source);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/some/path");
        
        CorsConfiguration config = source.getCorsConfiguration(request);
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getAllowedOrigins(), List.of("http://localhost:3000"));
        Assert.assertEquals(config.getAllowedMethods(), List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        Assert.assertEquals(config.getAllowedHeaders(), List.of("*"));
        Assert.assertTrue(config.getAllowCredentials());
    }

    @Test
    public void testCorsConfigurationSourceWithMultipleOrigins() {
        org.springframework.web.cors.CorsConfigurationSource source = securityConfig.corsConfigurationSource("http://localhost:3000,http://example.com");
        Assert.assertNotNull(source);
        
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/some/path");
        
        CorsConfiguration config = source.getCorsConfiguration(request);
        Assert.assertNotNull(config);
        Assert.assertEquals(config.getAllowedOrigins(), List.of("http://localhost:3000", "http://example.com"));
    }
}
