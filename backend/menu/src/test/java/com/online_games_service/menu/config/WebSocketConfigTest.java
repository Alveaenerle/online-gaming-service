package com.online_games_service.menu.config;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WebSocketConfigTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private MessageBrokerRegistry brokerRegistry;

    @Mock
    private StompEndpointRegistry endpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private ChannelRegistration channelRegistration;

    private WebSocketConfig webSocketConfig;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketConfig = new WebSocketConfig(redisTemplate);
        // Set the @Value property via reflection
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", "http://localhost");
    }

    @Test
    public void shouldConfigureMessageBroker() {
        // When
        webSocketConfig.configureMessageBroker(brokerRegistry);

        // Then
        verify(brokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(brokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(brokerRegistry).setUserDestinationPrefix("/user");
    }

    @Test
    public void shouldRegisterStompEndpoints() {
        // Given
        when(endpointRegistry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns(any(String[].class))).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any())).thenReturn(endpointRegistration);

        // When
        webSocketConfig.registerStompEndpoints(endpointRegistry);

        // Then
        verify(endpointRegistry).addEndpoint("/ws");
        verify(endpointRegistration).withSockJS();
    }

    @Test
    public void shouldConfigureClientInboundChannel() {
        // When
        webSocketConfig.configureClientInboundChannel(channelRegistration);

        // Then
        verify(channelRegistration).interceptors(any(WebSocketUserInterceptor.class));
    }
}
