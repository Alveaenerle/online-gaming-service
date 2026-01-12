export type LobbyPlayer = {
  userId: string;
  username: string;
  avatar: string;
  isReady: boolean;
  isHost: boolean;
  isYou: boolean;
};

export type PlayerStateRaw = {
  username: string;
  ready: boolean;
  avatarId: string;
};

export type LobbyInfoRaw = {
  id: string;
  gameType: string;
  maxPlayers: number;
  hostUserId: string;
  hostUsername?: string;
  players: Record<string, PlayerStateRaw>;
  status: "WAITING" | "PLAYING";
  name: string;
  accessCode: string;
  isPrivate?: boolean;
  private?: boolean;
};

export type LobbyInfo = {
  roomId: string;
  gameType: string;
  maxPlayers: number;
  players: LobbyPlayer[];
  gameStarted: boolean;
  name: string;
  accessCode: string;
  isPrivate?: boolean;
};
