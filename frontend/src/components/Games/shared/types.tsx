export interface LobbyPlayer {
  id: string;
  username: string;
  avatar: string;
  isHost?: boolean;
  isReady?: boolean;
  isYou?: boolean;
}

export interface GameLobbyConfig {
  gameKey: string;
  title: string;
  minPlayers: number;
  maxPlayers: number;
}
