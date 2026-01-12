import { LobbyInfo, LobbyInfoRaw, LobbyPlayer } from "./types";

export function mapLobbyRawToLobby(
  raw: LobbyInfoRaw,
  currentUserId?: string
): LobbyInfo {
  // Normalize IDs to lower-case strings to ensure safe comparison
  const rawHostId = (raw.hostUserId || "").toString().toLowerCase().trim();
  const currentId = (currentUserId || "").toString().toLowerCase().trim();
  const rawHostName = raw.hostUsername;

  const players: LobbyPlayer[] = Object.entries(raw.players).map(
    ([keyId, playerState]) => {
      const pId = keyId.toString().toLowerCase().trim();

      // Determine 'isYou'
      const isYou = !!currentId && currentId === pId;

      // Determine 'isHost'
      // 1. Check ID match
      let isHost = rawHostId === pId;

      // 2. Fallback: Host Username match (only if IDs don't match, e.g. guest anomalies)
      if (!isHost && rawHostName && playerState.username === rawHostName) {
        isHost = true;
      }
      
      // 3. Safety Override: If this is 'You' and 'You' are supposed to be host (by ID match against current user)
      // This covers cases where rawHostId matches currentId, but pId somehow didn't match rawHostId
      if (isYou && rawHostId === currentId) {
        isHost = true;
      }

      return {
        userId: keyId, // Keep original casing for display
        username: playerState.username,
        avatar: `/avatars/${playerState.avatarId}`,
        isHost: isHost,
        isYou: isYou,
        isReady: playerState.ready,
      };
    }
  );

  return {
    roomId: raw.id,
    gameType: raw.gameType,
    maxPlayers: raw.maxPlayers,
    players: players,
    gameStarted: raw.status === "PLAYING",
    name: raw.name,
    accessCode: raw.accessCode,
    isPrivate: raw.isPrivate ?? raw.private ?? false,
  };
}
