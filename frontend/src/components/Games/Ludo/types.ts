import { Color } from "./Board/constants";

export enum RoomStatus {
  WAITING = "WAITING",
  IN_GAME = "IN_GAME",
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
}
