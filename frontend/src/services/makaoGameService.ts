const API_URL = import.meta.env.VITE_MAKAO_API_URL ?? "/api/makao";

/**
 * Request to play a card
 * - cardSuit: The suit of the card (e.g., "HEARTS", "DIAMONDS", "CLUBS", "SPADES")
 * - cardRank: The rank of the card (e.g., "TWO", "THREE", ... "ACE")
 * - requestRank: Optional demanded rank when playing Jack
 * - requestSuit: Optional demanded suit when playing Ace
 */
interface PlayCardRequest {
  cardSuit: string;
  cardRank: string;
  requestRank?: string;
  requestSuit?: string;
}

/**
 * Response from drawing a card
 */
interface DrawCardResponse {
  card: { suit: string; rank: string } | null;
  isPlayable: boolean;
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
    let errorMessage = "Błąd API Makao";
    try {
      const error = await res.json();
      if (typeof error?.message === "string") {
        errorMessage = error.message;
      }
    } catch (parseError) {
      console.warn("Failed to parse error response as JSON:", parseError);
    }
    throw new Error(errorMessage);
  }

  return res.json() as Promise<T>;
}

export const makaoGameService = {
  /**
   * Request current game state - triggers WebSocket broadcast to the player
   */
  requestState(roomId: string): Promise<{ message: string }> {
    return request<{ message: string }>(`/state?roomId=${encodeURIComponent(roomId)}`, {
      method: "GET",
    });
  },

  /**
   * Play a card from hand
   */
  playCard(cardRequest: PlayCardRequest): Promise<{ message: string }> {
    return request<{ message: string }>("/play-card", {
      method: "POST",
      body: JSON.stringify(cardRequest),
    });
  },

  /**
   * Draw a card from the deck
   * Returns the drawn card and whether it can be played
   */
  drawCard(): Promise<DrawCardResponse> {
    return request<{ drawnCard: { suit: string; rank: string } | null; playable: boolean }>("/draw-card", {
      method: "POST",
    }).then((response) => ({
      card: response.drawnCard,
      isPlayable: response.playable,
    }));
  },

  /**
   * Play the card that was just drawn
   */
  playDrawnCard(cardRequest?: PlayCardRequest): Promise<{ message: string }> {
    return request<{ message: string }>("/play-drawn-card", {
      method: "POST",
      body: cardRequest ? JSON.stringify(cardRequest) : undefined,
    });
  },

  /**
   * Skip turn after drawing a card (when it cannot be played)
   */
  skipDrawnCard(): Promise<{ message: string }> {
    return request<{ message: string }>("/skip-drawn-card", {
      method: "POST",
    });
  },

  /**
   * Accept special effect (e.g., draw multiple cards from 2/3)
   */
  acceptEffect(): Promise<{ message: string }> {
    return request<{ message: string }>("/accept-effect", {
      method: "POST",
    });
  },
};
