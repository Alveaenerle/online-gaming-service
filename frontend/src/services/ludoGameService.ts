import { LudoGameStateMessage } from "../components/Games/Ludo/types"; // Zaimportuj swoje typy

const API_URL = import.meta.env.VITE_LUDO_API_URL ?? "/api/ludo";

async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  if (!res.ok) {
    let errorMessage = "Ludo API error";
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

  const contentType = res.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    return res.json() as Promise<T>;
  }
  return { message: "Success" } as unknown as T;
}

export const ludoService = {
  getGameState(): Promise<LudoGameStateMessage> {
    return request<LudoGameStateMessage>("/game-state");
  },

  rollDice(): Promise<{ message: string }> {
    return request<{ message: string }>("/roll", {
      method: "POST",
    });
  },

  movePawn(pawnIndex: number): Promise<{ message: string }> {
    return request<{ message: string }>(`/move?pawnIndex=${pawnIndex}`, {
      method: "POST",
    });
  },
};
