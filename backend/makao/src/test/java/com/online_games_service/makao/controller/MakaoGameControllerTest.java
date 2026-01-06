package com.online_games_service.makao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online_games_service.common.enums.CardRank;
import com.online_games_service.common.enums.CardSuit;
import com.online_games_service.common.model.Card;
import com.online_games_service.makao.dto.DrawCardResponse;
import com.online_games_service.makao.dto.PlayCardRequest;
import com.online_games_service.makao.service.MakaoGameService;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MakaoGameControllerTest {

	@Mock
	private MakaoGameService makaoGameService;

	private MakaoGameController controller;

	private MockMvc mockMvc;
	private AutoCloseable mocks;
	private final ObjectMapper mapper = new ObjectMapper();

	@BeforeMethod
	public void setUp() {
		mocks = MockitoAnnotations.openMocks(this);
		controller = new MakaoGameController(makaoGameService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@AfterMethod
	public void tearDown() throws Exception {
		if (mocks != null) {
			mocks.close();
		}
	}

	@Test
	public void playCard_requiresAuthentication() throws Exception {
		PlayCardRequest request = buildPlayRequest();

		mockMvc.perform(post("/play-card")
						.contentType(MediaType.APPLICATION_JSON)
						.content(mapper.writeValueAsString(request)))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(makaoGameService);
	}

	@Test
	public void playCard_successfulCall() throws Exception {
		PlayCardRequest request = buildPlayRequest();

		mockMvc.perform(post("/play-card")
						.contentType(MediaType.APPLICATION_JSON)
						.requestAttr("userId", "user-1")
						.content(mapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("success"));

		verify(makaoGameService).playCard(ArgumentMatchers.any(PlayCardRequest.class), ArgumentMatchers.eq("user-1"));
	}

	@Test
	public void playCard_conflictOnIllegalState() throws Exception {
		PlayCardRequest request = buildPlayRequest();
		doThrow(new IllegalStateException("not your turn"))
				.when(makaoGameService).playCard(ArgumentMatchers.any(PlayCardRequest.class), ArgumentMatchers.eq("user-1"));

		mockMvc.perform(post("/play-card")
						.contentType(MediaType.APPLICATION_JSON)
						.requestAttr("userId", "user-1")
						.content(mapper.writeValueAsString(request)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Game Error"))
				.andExpect(jsonPath("$.message").value("not your turn"));
	}

	@Test
	public void playCard_badRequestOnIllegalArgument() throws Exception {
		PlayCardRequest request = buildPlayRequest();
		doThrow(new IllegalArgumentException("missing card"))
				.when(makaoGameService).playCard(ArgumentMatchers.any(PlayCardRequest.class), ArgumentMatchers.eq("user-1"));

		mockMvc.perform(post("/play-card")
						.contentType(MediaType.APPLICATION_JSON)
						.requestAttr("userId", "user-1")
						.content(mapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation Error"))
				.andExpect(jsonPath("$.message").value("missing card"));
	}

	@Test
	public void drawCard_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/draw-card"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(makaoGameService);
	}

	@Test
	public void drawCard_returnsResponseBody() throws Exception {
		DrawCardResponse response = new DrawCardResponse(new Card(CardSuit.SPADES, CardRank.ACE), true);
		when(makaoGameService.drawCard("user-1")).thenReturn(response);

		mockMvc.perform(post("/draw-card").requestAttr("userId", "user-1"))
				.andExpect(status().isOk())
				.andExpect(content().json(mapper.writeValueAsString(response)));
	}

	@Test
	public void playDrawnCard_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/play-drawn-card"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(makaoGameService);
	}

	@Test
	public void playDrawnCard_successfulCall() throws Exception {
		PlayCardRequest request = buildPlayRequest();

		mockMvc.perform(post("/play-drawn-card")
						.contentType(MediaType.APPLICATION_JSON)
						.requestAttr("userId", "user-1")
						.content(mapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("success"));

		verify(makaoGameService).playDrawnCard(ArgumentMatchers.any(PlayCardRequest.class), ArgumentMatchers.eq("user-1"));
	}

	@Test
	public void skipDrawnCard_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/skip-drawn-card"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(makaoGameService);
	}

	@Test
	public void skipDrawnCard_successfulCall() throws Exception {
		mockMvc.perform(post("/skip-drawn-card").requestAttr("userId", "user-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("success"));

		verify(makaoGameService).skipDrawnCard("user-1");
	}

	@Test
	public void acceptEffect_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/accept-effect"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(makaoGameService);
	}

	@Test
	public void acceptEffect_successfulCall() throws Exception {
		mockMvc.perform(post("/accept-effect").requestAttr("userId", "user-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("success"));

		verify(makaoGameService).acceptEffect("user-1");
	}

	private PlayCardRequest buildPlayRequest() {
		PlayCardRequest request = new PlayCardRequest();
		request.setCardRank(CardRank.FIVE);
		request.setCardSuit(CardSuit.HEARTS);
		request.setRequestRank(CardRank.SEVEN);
		request.setRequestSuit(CardSuit.SPADES);
		return request;
	}
}
