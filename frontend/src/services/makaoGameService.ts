import {
  PlayCardRequest,
  DrawCardResponse,
  CardSuit,
  CardRank,
} from "../components/Games/Makao/types";

const API_URL = import.meta.env.VITE_MAKAO_API_URL ?? "/api/makao";

interface ApiError {
  error: string;
  message: string;
}

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  if (!res.ok) {
    let errorMessage = "Makao API error";
    try {
      const error: ApiError = await res.json();
      if (typeof error?.message === "string") {
        errorMessage = error.message;
      }
    } catch {
      // Ignore parse errors
    }
    throw new Error(errorMessage);
  }

  // Handle empty responses
  const text = await res.text();
  if (!text) {
    return {} as T;
  }

  return JSON.parse(text) as T;
}

export interface PlayCardPayload {
  cardSuit: CardSuit;
  cardRank: CardRank;
  requestSuit?: CardSuit | null;
  requestRank?: CardRank | null;
}

export interface PlayDrawnCardPayload {
  requestSuit?: CardSuit | null;
  requestRank?: CardRank | null;
}

export const makaoGameService = {
  /**
   * Play a card from hand
   */
  playCard(payload: PlayCardPayload): Promise<{ message: string }> {
    const request_body: PlayCardRequest = {
      cardSuit: payload.cardSuit,
      cardRank: payload.cardRank,
      requestSuit: payload.requestSuit ?? null,
      requestRank: payload.requestRank ?? null,
    };

    return request<{ message: string }>("/play-card", {
      method: "POST",
      body: JSON.stringify(request_body),
    });
  },

  /**
   * Draw a card from deck
   */
  drawCard(): Promise<DrawCardResponse> {
    return request<DrawCardResponse>("/draw-card", {
      method: "POST",
    });
  },

  /**
   * Play the drawn card (if playable)
   */
  playDrawnCard(payload?: PlayDrawnCardPayload): Promise<{ message: string }> {
    const body = payload
      ? JSON.stringify({
          requestSuit: payload.requestSuit ?? null,
          requestRank: payload.requestRank ?? null,
        })
      : undefined;

    return request<{ message: string }>("/play-drawn-card", {
      method: "POST",
      body,
    });
  },

  /**
   * Skip turn after drawing (keep the drawn card)
   */
  skipDrawnCard(): Promise<{ message: string }> {
    return request<{ message: string }>("/skip-drawn-card", {
      method: "POST",
    });
  },

  /**
   * Accept a special effect (draw cards or skip turns)
   */
  acceptEffect(): Promise<{ message: string }> {
    return request<{ message: string }>("/accept-effect", {
      method: "POST",
    });
  },

  /**
   * Request the current game state to be sent via WebSocket.
   * Useful after connecting/reconnecting to get the current state.
   */
  requestState(): Promise<{ message: string }> {
    return request<{ message: string }>("/request-state", {
      method: "POST",
    });
  },
};

export default makaoGameService;
