package com.online_games_service.menu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.enums.RoomStatus;
import com.online_games_service.menu.dto.CreateRoomRequest;
import com.online_games_service.menu.dto.JoinGameRequest;
import com.online_games_service.menu.dto.KickPlayerRequest;
import com.online_games_service.menu.dto.RoomInfoResponse;
import com.online_games_service.menu.model.GameRoom;
import com.online_games_service.menu.model.PlayerState;
import com.online_games_service.menu.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.MockMvc;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
public class GameRoomControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GameRoomService gameRoomService;

    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    public void shouldReturnRoomInfoWhenAuthorized() throws Exception {
        RoomInfoResponse response = sampleRoomInfo();
        when(gameRoomService.getPlayerRoomInfo("alice-id", "alice")).thenReturn(response);

        mockMvc.perform(get("/room-info").requestAttr("userId", "alice-id").requestAttr("username", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(response.getId()))
            .andExpect(jsonPath("$.hostUsername").value("host"))
            .andExpect(jsonPath("$.hostUserId").value("host-id"))
            .andExpect(jsonPath("$.players.host-id").value("host"));
    }

    @Test
    public void shouldRejectRoomInfoWhenUnauthorized() throws Exception {
        mockMvc.perform(get("/room-info"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldCreateRoomWhenAuthorized() throws Exception {
        CreateRoomRequest request = sampleCreateRequest();
        GameRoom room = new GameRoom("Room", GameType.LUDO, "host-id", "host", 4, false);
        room.setId("room-1");
        when(gameRoomService.createRoom(any(CreateRoomRequest.class), eq("host-id"), eq("host"))).thenReturn(room);

        mockMvc.perform(post("/create")
                .requestAttr("userId", "host-id")
                .requestAttr("username", "host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("room-1"));
    }

    @Test
    public void shouldRejectCreateRoomWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldJoinRoomWhenAuthorized() throws Exception {
        JoinGameRequest request = sampleJoinRequest();
        GameRoom room = new GameRoom("Room", GameType.LUDO, "host-id", "host", 4, false);
        room.setId("room-2");
        when(gameRoomService.joinRoom(any(JoinGameRequest.class), eq("player-id"), eq("player"))).thenReturn(room);

        mockMvc.perform(post("/join")
                .requestAttr("userId", "player-id")
                .requestAttr("username", "player")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("room-2"));
    }

    @Test
    public void shouldRejectJoinRoomWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleJoinRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldStartGameWhenAuthorized() throws Exception {
        GameRoom room = new GameRoom("Room", GameType.LUDO, "host-id", "host", 4, false);
        room.setId("room-3");
        room.setStatus(RoomStatus.PLAYING);
        when(gameRoomService.startGame("host-id", "host")).thenReturn(room);

        mockMvc.perform(post("/start").requestAttr("userId", "host-id").requestAttr("username", "host"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    public void shouldRejectStartGameWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/start"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldLeaveRoomWhenAuthorized() throws Exception {
        when(gameRoomService.leaveRoom("player-id", "player")).thenReturn("Left room");

        mockMvc.perform(post("/leave").requestAttr("userId", "player-id").requestAttr("username", "player"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Left room"));

        verify(gameRoomService).leaveRoom("player-id", "player");
    }

    @Test
    public void shouldRejectLeaveRoomWhenUnauthorized() throws Exception {
        mockMvc.perform(post("/leave"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldKickPlayerWhenAuthorized() throws Exception {
        KickPlayerRequest request = new KickPlayerRequest();
        request.setUserId("guest-id");
        when(gameRoomService.kickPlayer("host-id", "host", "guest-id")).thenReturn("Player guest removed");

        mockMvc.perform(post("/kick-player")
                .requestAttr("userId", "host-id")
                .requestAttr("username", "host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Player guest removed"));
    }

    @Test
    public void shouldRejectKickPlayerWhenUnauthorized() throws Exception {
        KickPlayerRequest request = new KickPlayerRequest();
        request.setUserId("guest-id");

        mockMvc.perform(post("/kick-player")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void shouldFailValidationWhenKickRequestInvalid() throws Exception {
        mockMvc.perform(post("/kick-player")
                        .requestAttr("userId", "host-id")
                        .requestAttr("username", "host")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldMapIllegalStateToConflict() throws Exception {
        when(gameRoomService.startGame("host-id", "host")).thenThrow(new IllegalStateException("Already started"));

        mockMvc.perform(post("/start").requestAttr("userId", "host-id").requestAttr("username", "host"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Game Error"))
                .andExpect(jsonPath("$.message").value("Already started"));
    }

    @Test
    public void shouldMapIllegalArgumentToBadRequest() throws Exception {
        when(gameRoomService.joinRoom(any(JoinGameRequest.class), eq("player-id"), eq("player")))
                .thenThrow(new IllegalArgumentException("Bad data"));

        mockMvc.perform(post("/join")
                .requestAttr("userId", "player-id")
                .requestAttr("username", "player")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleJoinRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.message").value("Bad data"));
    }

    private CreateRoomRequest sampleCreateRequest() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Fun room");
        request.setGameType(GameType.LUDO);
        request.setMaxPlayers(4);
        request.setPrivate(false);
        return request;
    }

    private JoinGameRequest sampleJoinRequest() {
        JoinGameRequest request = new JoinGameRequest();
        request.setGameType(GameType.LUDO);
        request.setMaxPlayers(4);
        request.setRandom(true);
        request.setAccessCode(null);
        return request;
    }

    private RoomInfoResponse sampleRoomInfo() {
        return new RoomInfoResponse(
                "room-info",
                "Room",
                GameType.LUDO,
                Map.of("host-id", PlayerState.createDefault("host")),
                4,
                false,
                "CODE1",
                "host-id",
                "host",
                RoomStatus.WAITING);
    }
}
