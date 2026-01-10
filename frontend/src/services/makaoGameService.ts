const API_URL = import.meta.env.VITE_MAKAO_API_URL ?? "/api/makao";

interface PlayCardRequest {
  cardSuit: string;
  cardRank: string;
  demandedRank?: string;
  demandedSuit?: string;
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
  playCard(cardRequest: PlayCardRequest): Promise<{ message: string }> {
    return request<{ message: string }>("/play-card", {
      method: "POST",
      body: JSON.stringify(cardRequest),
    });
  },

  drawCard(): Promise<{ card: { suit: string; rank: string } | null }> {
    return request<{ card: { suit: string; rank: string } | null }>("/draw-card", {
      method: "POST",
    });
  },

  playDrawnCard(cardRequest?: PlayCardRequest): Promise<{ message: string }> {
    return request<{ message: string }>("/play-drawn-card", {
      method: "POST",
      body: cardRequest ? JSON.stringify(cardRequest) : undefined,
    });
  },

  skipDrawnCard(): Promise<{ message: string }> {
    return request<{ message: string }>("/skip-drawn-card", {
      method: "POST",
    });
  },

  acceptEffect(): Promise<{ message: string }> {
    return request<{ message: string }>("/accept-effect", {
      method: "POST",
    });
  },
};
