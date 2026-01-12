import { Color } from "./Board/constants";

export enum RoomStatus {
  WAITING = "WAITING",
  IN_GAME = "IN_GAME",
  FINISHED = "FINISHED",
}

export interface LudoPawn {
  id: number; // odpowiednik int id
  position: number; // odpowiednik int position
  color: Color; // enum PlayerColor
  stepsMoved: number; // int stepsMoved
  inBase: boolean; // boolean inBase
  inHome: boolean; // boolean inHome (czy jest na mecie/w środku)
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
