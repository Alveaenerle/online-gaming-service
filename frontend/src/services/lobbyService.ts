import { LobbyInfoRaw } from "../components/Games/utils/types";

const API_URL = import.meta.env.VITE_MENU_API_URL ?? "/api/menu";

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
    } catch (parseError) {
      console.warn("Failed to parse error response as JSON:", parseError);
    }
    throw new Error(errorMessage);
  }

  return res.json() as Promise<T>;
}

export const lobbyService = {
  getRoomInfo(): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/room-info");
  },

  createRoom(
    gameType: string,
    maxPlayers: number,
    name?: string,
    isPrivate: boolean = false
  ): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/create", {
      method: "POST",
      body: JSON.stringify({
        gameType,
        maxPlayers,
        name: name || "Default Room Name",
        isPrivate,
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

  updateAvatar(avatarId: string): Promise<LobbyInfoRaw> {
    return request<LobbyInfoRaw>("/update-avatar", {
      method: "POST",
      body: JSON.stringify({ avatarId }),
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
