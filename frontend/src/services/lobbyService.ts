import { LobbyInfoRaw } from "../components/Games/shared/types";

/* ===== API URL ===== */
const API_URL = import.meta.env.VITE_API_URL ?? "/api/menu";

/* ===== GENERIC REQUEST ===== */
async function request<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_URL}${endpoint}`, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  if (!res.ok) {
    let errorMessage = "Lobby API error";
    try {
      const error = await res.json();
      if (typeof error?.message === "string") {
        errorMessage = error.message;
      }
    } catch {
      /* ignore */
    }
    throw new Error(errorMessage);
  }

  return res.json() as Promise<T>;
}

/* ===== SERVICE ===== */
export const lobbyService = {
  /* ===== ROOM INFO ===== */
  getRoomInfo(): Promise<LobbyInfoRaw> {
    // backend zwraca lobby przypisane do sesji użytkownika
    return request<LobbyInfoRaw>("/room-info");
  },

  /* ===== CREATE / JOIN ===== */
  createRoom(
    gameType: string,
    maxPlayers: number,
    name?: string
  ): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/create", {
      method: "POST",
      body: JSON.stringify({
        gameType,
        maxPlayers,
        name: name || "Default Room Name",
      }),
    });
  },

  joinRoom(
    accessCode: string,
    maxPlayers = 4,
    gameType = "MAKAO",
    isRandom = false
  ): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/join", {
      method: "POST",
      body: JSON.stringify({
        gameType,
        maxPlayers,
        isRandom,
        accessCode: accessCode.trim(),
      }),
    });
  },

  /* ===== GAME FLOW ===== */
  startGame(): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/start", {
      method: "POST",
    });
  },

  toggleReady(): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/ready", {
      method: "POST",
    });
  },

  leaveRoom(): Promise<{ message: string }> {
    return request<{ message: string }>("/leave", {
      method: "POST",
    });
  },

  kickPlayer(userId: string): Promise<{ message: string }> {
    return request<{ message: string }>("/kick-player", {
      method: "POST",
      body: JSON.stringify({ userId }),
    });
  },
};
