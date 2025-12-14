export type Suit = "hearts" | "diamonds" | "clubs" | "spades";
export type Rank = "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" | "10" | "J" | "Q" | "K" | "A";

export interface Card {
  suit: Suit;
  rank: Rank;
  id: string;
}

export interface Player {
  id: number;
  name: string;
  cards: Card[];
  isHuman: boolean;
}

export interface GameState {
  players: Player[];
  currentPlayerIndex: number;
  deck: Card[];
  discardPile: Card[];
  direction: 1 | -1; // 1 = clockwise, -1 = counterclockwise
  drawCount: number; // for special cards like 2 or 3
  requestedRank?: Rank; // when Jack is played
  requestedSuit?: Suit; // when Ace is played
  gameStarted: boolean;
  winner: number | null;
}
