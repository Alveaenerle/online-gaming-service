package com.online_games_service.menu.controller;

import com.online_games_service.menu.config.WebSocketUserInterceptor.WebSocketPrincipal;
import com.online_games_service.menu.dto.chat.*;
import com.online_games_service.menu.model.ChatMessage;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.model.PlayerState;
import com.online_games_service.menu.service.chat.ChatRateLimiterService;
import com.online_games_service.menu.service.chat.ChatService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.Principal;
import java.time.Instant;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private ChatRateLimiterService rateLimiter;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ChatController chatController;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        chatController = new ChatController(chatService, rateLimiter, messagingTemplate, redisTemplate);
    }

    // ============= sendMessage tests =============

    @Test
    public void sendMessage_shouldDoNothing_whenPrincipalIsNull() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        chatController.sendMessage("lobby-1", request, null);

        verifyNoInteractions(chatService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    public void sendMessage_shouldSendError_whenUserNotInLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn(null);

        chatController.sendMessage("lobby-1", request, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                any(ChatErrorResponse.class)
        );
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSendError_whenUserInDifferentLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("different-lobby");

        chatController.sendMessage("lobby-1", request, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                any(ChatErrorResponse.class)
        );
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSendRateLimitError_whenRateLimited() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.of(5000L));

        chatController.sendMessage("lobby-1", request, principal);

        ArgumentCaptor<ChatErrorResponse> errorCaptor = ArgumentCaptor.forClass(ChatErrorResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                errorCaptor.capture()
        );

        ChatErrorResponse error = errorCaptor.getValue();
        assertEquals(error.getCode(), "RATE_LIMIT");
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSendError_whenMessageIsEmpty() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());

        chatController.sendMessage("lobby-1", request, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                any(ChatErrorResponse.class)
        );
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSendError_whenMessageIsNull() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent(null);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());

        chatController.sendMessage("lobby-1", request, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                any(ChatErrorResponse.class)
        );
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSendError_whenMessageTooLong() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("a".repeat(501));

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());

        chatController.sendMessage("lobby-1", request, principal);

        verify(messagingTemplate).convertAndSendToUser(
                eq("user-123"),
                eq("/queue/chat/error"),
                any(ChatErrorResponse.class)
        );
        verifyNoInteractions(chatService);
    }

    @Test
    public void sendMessage_shouldSaveAndBroadcast_whenValidMessage() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello everyone!");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());
        when(valueOperations.get("game:room:lobby-1")).thenReturn(null);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId("lobby-1")
                .senderId("user-123")
                .senderUsername("TestUser")
                .content("Hello everyone!")
                .timestamp(Instant.now())
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .build();
        when(chatService.saveMessage(eq("lobby-1"), eq("user-123"), eq("TestUser"), anyString(), eq("Hello everyone!")))
                .thenReturn(savedMessage);

        chatController.sendMessage("lobby-1", request, principal);

        verify(chatService).saveMessage("lobby-1", "user-123", "TestUser", "/avatars/avatar_1.png", "Hello everyone!");
        verify(messagingTemplate).convertAndSend(eq("/topic/room/lobby-1/chat"), any(ChatMessageResponse.class));
    }

    @Test
    public void sendMessage_shouldGetAvatarFromRoom_whenRoomExists() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello!");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());

        GameRoom room = new GameRoom();
        room.setId("lobby-1");
        Map<String, PlayerState> players = new HashMap<>();
        PlayerState player = new PlayerState("TestUser", false, "/avatars/custom.png");
        players.put("user-123", player);
        room.setPlayers(players);
        when(valueOperations.get("game:room:lobby-1")).thenReturn(room);
        when(chatService.getPlayerAvatar(any(), eq("user-123"))).thenReturn("/avatars/custom.png");

        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId("lobby-1")
                .senderId("user-123")
                .senderUsername("TestUser")
                .content("Hello!")
                .timestamp(Instant.now())
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .build();
        when(chatService.saveMessage(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(savedMessage);

        chatController.sendMessage("lobby-1", request, principal);

        verify(chatService).saveMessage("lobby-1", "user-123", "TestUser", "/avatars/custom.png", "Hello!");
    }

    // ============= handleTyping tests =============

    @Test
    public void handleTyping_shouldDoNothing_whenPrincipalIsNull() {
        TypingIndicator indicator = TypingIndicator.builder().isTyping(true).build();

        chatController.handleTyping("lobby-1", indicator, null);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    public void handleTyping_shouldDoNothing_whenUserNotInLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        TypingIndicator indicator = TypingIndicator.builder().isTyping(true).build();

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn(null);

        chatController.handleTyping("lobby-1", indicator, principal);

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(TypingIndicator.class));
    }

    @Test
    public void handleTyping_shouldBroadcast_whenUserInLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        TypingIndicator indicator = TypingIndicator.builder().isTyping(true).build();

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");

        chatController.handleTyping("lobby-1", indicator, principal);

        ArgumentCaptor<TypingIndicator> captor = ArgumentCaptor.forClass(TypingIndicator.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/lobby-1/typing"), captor.capture());

        TypingIndicator broadcast = captor.getValue();
        assertEquals(broadcast.getUserId(), "user-123");
        assertEquals(broadcast.getUsername(), "TestUser");
        assertTrue(broadcast.isTyping());
    }

    // ============= getChatHistory tests =============

    @Test
    public void getChatHistory_shouldReturnEmpty_whenPrincipalIsNull() {
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(0);
        request.setLimit(50);

        ChatHistoryResponse response = chatController.getChatHistory("lobby-1", request, null);

        assertNotNull(response);
        verifyNoInteractions(chatService);
    }

    @Test
    public void getChatHistory_shouldReturnEmpty_whenUserNotInLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(0);
        request.setLimit(50);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn(null);

        ChatHistoryResponse response = chatController.getChatHistory("lobby-1", request, principal);

        assertNotNull(response);
        verifyNoInteractions(chatService);
    }

    @Test
    public void getChatHistory_shouldReturnHistory_whenUserInLobby() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(0);
        request.setLimit(50);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");

        ChatHistoryResponse expectedResponse = ChatHistoryResponse.builder()
                .messages(List.of())
                .hasMore(false)
                .totalMessages(0)
                .build();
        when(chatService.getChatHistory("lobby-1", 0, 50)).thenReturn(expectedResponse);

        ChatHistoryResponse response = chatController.getChatHistory("lobby-1", request, principal);

        assertEquals(response, expectedResponse);
        verify(chatService).getChatHistory("lobby-1", 0, 50);
    }

    @Test
    public void getChatHistory_shouldClampNegativeOffset() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(-10);
        request.setLimit(50);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(chatService.getChatHistory("lobby-1", 0, 50)).thenReturn(ChatHistoryResponse.builder().build());

        chatController.getChatHistory("lobby-1", request, principal);

        verify(chatService).getChatHistory("lobby-1", 0, 50);
    }

    @Test
    public void getChatHistory_shouldClampLimitToMax100() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(0);
        request.setLimit(500);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(chatService.getChatHistory("lobby-1", 0, 100)).thenReturn(ChatHistoryResponse.builder().build());

        chatController.getChatHistory("lobby-1", request, principal);

        verify(chatService).getChatHistory("lobby-1", 0, 100);
    }

    @Test
    public void getChatHistory_shouldClampLimitToMin1() {
        WebSocketPrincipal principal = new WebSocketPrincipal("user-123", "TestUser");
        ChatHistoryRequest request = new ChatHistoryRequest();
        request.setOffset(0);
        request.setLimit(0);

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(chatService.getChatHistory("lobby-1", 0, 1)).thenReturn(ChatHistoryResponse.builder().build());

        chatController.getChatHistory("lobby-1", request, principal);

        verify(chatService).getChatHistory("lobby-1", 0, 1);
    }

    // ============= getUsername tests =============

    @Test
    public void sendMessage_shouldUseUnknown_whenPrincipalNotWebSocketPrincipal() {
        // Using a regular Principal, not WebSocketPrincipal
        Principal regularPrincipal = () -> "user-123";
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Hello!");

        when(valueOperations.get("game:user-room:id:user-123")).thenReturn("lobby-1");
        when(rateLimiter.checkRateLimit("user-123")).thenReturn(Optional.empty());
        when(valueOperations.get("game:room:lobby-1")).thenReturn(null);

        ChatMessage savedMessage = ChatMessage.builder()
                .id(UUID.randomUUID().toString())
                .lobbyId("lobby-1")
                .senderId("user-123")
                .senderUsername("Unknown")
                .content("Hello!")
                .timestamp(Instant.now())
                .type(ChatMessage.MessageType.USER_MESSAGE)
                .build();
        when(chatService.saveMessage(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(savedMessage);

        chatController.sendMessage("lobby-1", request, regularPrincipal);

        verify(chatService).saveMessage("lobby-1", "user-123", "Unknown", "/avatars/avatar_1.png", "Hello!");
    }
}
