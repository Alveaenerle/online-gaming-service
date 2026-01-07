export type LobbyPlayer = {
  userId: string;
  username: string;
  avatar: string;
  isReady: boolean;
  isHost: boolean;
  isYou: boolean;
};

export type LobbyInfoRaw = {
  id: string;
  gameType: string;
  maxPlayers: number;
  hostUserId: string;
  players: Record<string, string>;
  status: "WAITING" | "STARTED";
  name: string;
  accessCode: string;
};

export type LobbyInfo = {
  roomId: string;
  gameType: string;
  maxPlayers: number;
  players: LobbyPlayer[];
  gameStarted: boolean;
  name: string;
  accessCode: string;
};
