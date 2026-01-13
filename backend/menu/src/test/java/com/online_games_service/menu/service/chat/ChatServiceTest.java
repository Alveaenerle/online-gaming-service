package com.online_games_service.menu.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.online_games_service.menu.dto.chat.ChatHistoryResponse;
import com.online_games_service.menu.model.ChatMessage;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ChatServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ListOperations<String, Object> listOperations;

    private ChatService chatService;
    private ProfanityFilterService profanityFilter;
    private ObjectMapper objectMapper;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        
        profanityFilter = new ProfanityFilterService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        chatService = new ChatService(redisTemplate, objectMapper, profanityFilter);
    }

    @Test
    public void shouldSaveCleanMessage() {
        String lobbyId = "lobby-123";
        String userId = "user-456";
        String username = "TestUser";
        String avatar = "/avatars/avatar_1.png";
        String content = "Hello everyone!";

        when(listOperations.rightPush(anyString(), any())).thenReturn(1L);

        ChatMessage result = chatService.saveMessage(lobbyId, userId, username, avatar, content);

        Assert.assertNotNull(result.getId());
        Assert.assertEquals(result.getLobbyId(), lobbyId);
        Assert.assertEquals(result.getSenderId(), userId);
        Assert.assertEquals(result.getSenderUsername(), username);
        Assert.assertEquals(result.getContent(), content);
        Assert.assertFalse(result.isBlurred());
        Assert.assertEquals(result.getType(), ChatMessage.MessageType.USER_MESSAGE);

        verify(listOperations).rightPush(eq("lobby:chat:" + lobbyId), anyString());
    }

    @Test
    public void shouldSaveAndFilterProfanity() {
        String lobbyId = "lobby-789";
        String content = "This is a damn message";

        when(listOperations.rightPush(anyString(), any())).thenReturn(1L);

        ChatMessage result = chatService.saveMessage(lobbyId, "user", "User", "/avatar", content);

        Assert.assertTrue(result.isBlurred());
        Assert.assertFalse(result.getContent().contains("damn"));
        Assert.assertTrue(result.getContent().contains("****"));
    }

    @Test
    public void shouldCreateSystemMessage() {
        String lobbyId = "lobby-sys";
        String content = "User joined the lobby";

        when(listOperations.rightPush(anyString(), any())).thenReturn(1L);

        ChatMessage result = chatService.createSystemMessage(lobbyId, content);

        Assert.assertEquals(result.getSenderId(), "SYSTEM");
        Assert.assertEquals(result.getSenderUsername(), "System");
        Assert.assertEquals(result.getContent(), content);
        Assert.assertEquals(result.getType(), ChatMessage.MessageType.SYSTEM_MESSAGE);
    }

    @Test
    public void shouldGetChatHistoryWithPagination() throws Exception {
        String lobbyId = "lobby-hist";
        
        // Create 50 messages
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ChatMessage msg = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .lobbyId(lobbyId)
                    .senderId("user-" + i)
                    .senderUsername("User" + i)
                    .senderAvatar("/avatars/avatar_1.png")
                    .content("Message " + i)
                    .timestamp(Instant.now())
                    .isBlurred(false)
                    .type(ChatMessage.MessageType.USER_MESSAGE)
                    .build();
            messages.add(objectMapper.writeValueAsString(msg));
        }

        when(listOperations.size(anyString())).thenReturn(50L);
        
        // First page (offset 0, limit 20) - should get messages 30-49 (newest)
        when(listOperations.range(anyString(), eq(30L), eq(49L)))
                .thenReturn(messages.subList(30, 50));

        ChatHistoryResponse response = chatService.getChatHistory(lobbyId, 0, 20);

        Assert.assertEquals(response.getMessages().size(), 20);
        Assert.assertEquals(response.getOffset(), 0);
        Assert.assertEquals(response.getLimit(), 20);
        Assert.assertTrue(response.isHasMore());
        Assert.assertEquals(response.getTotalMessages(), 50);
    }

    @Test
    public void shouldGetSecondPageOfHistory() throws Exception {
        String lobbyId = "lobby-page2";
        
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ChatMessage msg = ChatMessage.builder()
                    .id(UUID.randomUUID().toString())
                    .lobbyId(lobbyId)
                    .senderId("user-" + i)
                    .senderUsername("User" + i)
                    .content("Message " + i)
                    .timestamp(Instant.now())
                    .isBlurred(false)
                    .type(ChatMessage.MessageType.USER_MESSAGE)
                    .build();
            messages.add(objectMapper.writeValueAsString(msg));
        }

        when(listOperations.size(anyString())).thenReturn(50L);
        
        // Second page (offset 20, limit 20) - should get messages 10-29
        when(listOperations.range(anyString(), eq(10L), eq(29L)))
                .thenReturn(messages.subList(10, 30));

        ChatHistoryResponse response = chatService.getChatHistory(lobbyId, 20, 20);

        Assert.assertEquals(response.getMessages().size(), 20);
        Assert.assertEquals(response.getOffset(), 20);
        Assert.assertTrue(response.isHasMore());  // Still more messages before this
    }

    @Test
    public void shouldReturnEmptyHistoryForNonExistentLobby() {
        when(listOperations.size(anyString())).thenReturn(0L);

        ChatHistoryResponse response = chatService.getChatHistory("non-existent", 0, 20);

        Assert.assertTrue(response.getMessages().isEmpty());
        Assert.assertFalse(response.isHasMore());
        Assert.assertEquals(response.getTotalMessages(), 0);
    }

    @Test
    public void shouldDeleteChatHistory() {
        String lobbyId = "lobby-delete";

        chatService.deleteChatHistory(lobbyId);

        verify(redisTemplate).delete("lobby:chat:" + lobbyId);
    }
}
