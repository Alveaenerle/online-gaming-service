package com.online_games_service.ludo.controller;

import com.online_games_service.ludo.dto.LudoGameStateMessage;
import com.online_games_service.ludo.exception.GameLogicException;
import com.online_games_service.ludo.exception.InvalidMoveException;
import com.online_games_service.ludo.service.LudoService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LudoControllerTest {

    @Mock
    private LudoService ludoService;

    private LudoController controller;
    private MockMvc mockMvc;
    private AutoCloseable mocks;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        controller = new LudoController(ludoService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addPlaceholderValue("ludo.http.cors.allowed-origins", "http://localhost:3000")
                .build();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void getGame_shouldReturnGameState() throws Exception {
        // Given
        String userId = "user-1";
        LudoGameStateMessage state = new LudoGameStateMessage();
        state.setGameId("game-1");
        
        when(ludoService.getGameState(userId)).thenReturn(state);

        // When & Then
        mockMvc.perform(get("/game-state")
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("game-1"));
        
        verify(ludoService).getGameState(userId);
    }

    @Test
    public void rollDice_successfulCall() throws Exception {
        // Given
        String userId = "user-1";

        // When & Then
        mockMvc.perform(post("/roll")
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Dice rolled"));

        verify(ludoService).rollDice(userId);
    }

    @Test
    public void movePawn_successfulCall() throws Exception {
        // Given
        String userId = "user-1";
        int pawnIndex = 2;

        // When & Then
        mockMvc.perform(post("/move")
                        .param("pawnIndex", String.valueOf(pawnIndex))
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Move accepted"));

        verify(ludoService).movePawn(userId, pawnIndex);
    }

    @Test
    public void handleIllegalStateException_shouldReturnConflict() throws Exception {
        // Given
        doThrow(new IllegalStateException("Game is full"))
                .when(ludoService).rollDice(anyString());

        // When & Then
        mockMvc.perform(post("/roll")
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Game Error"))
                .andExpect(jsonPath("$.message").value("Game is full"));
    }

    @Test
    public void handleIllegalArgumentException_shouldReturnBadRequest() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Bad input"))
                .when(ludoService).getGameState(anyString());

        // When & Then
        mockMvc.perform(get("/game-state")
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Bad input"));
    }

    @Test
    public void handleGameLogicException_shouldReturnConflict() throws Exception {
        // Given
        doThrow(new GameLogicException("Not your turn"))
                .when(ludoService).rollDice(anyString());

        // When & Then
        mockMvc.perform(post("/roll")
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Game Logic Error"))
                .andExpect(jsonPath("$.message").value("Not your turn"));
    }

    @Test
    public void handleInvalidMoveException_shouldReturnBadRequest() throws Exception {
        // Given
        doThrow(new InvalidMoveException("Invalid move"))
                .when(ludoService).movePawn(anyString(), anyInt());

        // When & Then
        mockMvc.perform(post("/move")
                        .param("pawnIndex", "0")
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Move"))
                .andExpect(jsonPath("$.message").value("Invalid move"));
    }
}