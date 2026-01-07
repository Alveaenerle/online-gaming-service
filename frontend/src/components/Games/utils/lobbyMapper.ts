import { LobbyInfo, LobbyInfoRaw, LobbyPlayer } from "./types";

export function mapLobbyRawToLobby(
  raw: LobbyInfoRaw,
  currentUserId?: string
): LobbyInfo {
  const players: LobbyPlayer[] = Object.entries(raw.players).map(
    ([userId, username]) => {
      return {
        userId,
        username,
        avatar: `/avatars/avatar_1.png`,
        isHost: userId === raw.hostUserId,
        isYou: userId === currentUserId,
        isReady: true,
      };
    }
  );

  return {
    roomId: raw.id,
    gameType: raw.gameType,
    maxPlayers: raw.maxPlayers,
    players: players,
    gameStarted: raw.status === "STARTED",
    name: raw.name,
    accessCode: raw.accessCode,
  };
}
