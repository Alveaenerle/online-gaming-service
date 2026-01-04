package com.online_games_service.menu.messaging;

import com.online_games_service.common.enums.GameType;
import com.online_games_service.common.messaging.GameStartMessage;
import com.online_games_service.menu.model.GameRoom;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GameStartPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TopicExchange topicExchange;

    private AutoCloseable mocks;
    private GameStartPublisher publisher;

    @BeforeMethod
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(topicExchange.getName()).thenReturn("game.events");
        publisher = new GameStartPublisher(rabbitTemplate, topicExchange);
        ReflectionTestUtils.setField(publisher, "makaoRoutingKey", "makao.start");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void shouldPublishGameStartForMakao() {
        GameRoom room = new GameRoom("Room", GameType.MAKAO, "host", "Host", 4, false);
        room.setId("room-1");

        publisher.publish(room);

        ArgumentCaptor<GameStartMessage> payloadCaptor = ArgumentCaptor.forClass(GameStartMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("game.events"), eq("makao.start"), payloadCaptor.capture());

        GameStartMessage payload = payloadCaptor.getValue();
        Assert.assertEquals(payload.roomId(), "room-1");
        Assert.assertEquals(payload.roomName(), "Room");
        Assert.assertEquals(payload.gameType(), GameType.MAKAO);
        Assert.assertEquals(payload.players().size(), room.getPlayers().size());
        Assert.assertEquals(payload.players().get("host"), "Host");
        Assert.assertEquals(payload.maxPlayers(), 4);
        Assert.assertEquals(payload.hostUserId(), "host");
        Assert.assertEquals(payload.hostUsername(), "Host");
    }

    @Test
    public void shouldSkipPublishWhenRoutingKeyMissing() {
        GameRoom room = new GameRoom("Room", null, "host", "Host", 4, false);
        room.setId("room-2");

        publisher.publish(room);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(GameStartMessage.class));
    }

    @Test
    public void shouldRethrowWhenPublishFails() {
        GameRoom room = new GameRoom("Room", GameType.MAKAO, "host", "Host", 4, false);
        room.setId("room-3");

        doThrow(new AmqpException("broken"))
            .when(rabbitTemplate)
            .convertAndSend(eq("game.events"), eq("makao.start"), any(GameStartMessage.class));

        Assert.expectThrows(IllegalStateException.class, () -> publisher.publish(room));
    }
}
