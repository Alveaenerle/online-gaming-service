import { Color } from "./Board/constants";

export enum RoomStatus {
  WAITING = "WAITING",
  FULL = "FULL",
  PLAYING = "PLAYING",
  FINISHED = "FINISHED",
}

export interface LudoPawn {
  id: number;
  position: number;
  color: Color;
  stepsMoved: number;
  inBase: boolean;
  inHome: boolean;
}

export interface LudoPlayer {
  userId: string;
  color: Color;
  pawns: LudoPawn[];
  isBot: boolean;
}

export interface LudoGameStateMessage {
  gameId: string;
  status: RoomStatus;
  currentPlayerColor: Color;
  currentPlayerId: string;

  lastDiceRoll: number;
  diceRolled: boolean;
  waitingForMove: boolean;
  rollsLeft: number;

  players: LudoPlayer[];
  usernames: Record<string, string>;
  winnerId: string | null;
  capturedUserId: string | null;

  // Turn timer - seconds remaining for current player's turn (null for bots)
  turnRemainingSeconds?: number | null;
  // Turn start time in milliseconds (for accurate client-side timer calculation)
  turnStartTime?: number | null;
  // Player avatars - playerId -> avatarId (e.g., "avatar_1.png" or "bot_avatar.svg")
  playersAvatars?: Record<string, string>;
}
