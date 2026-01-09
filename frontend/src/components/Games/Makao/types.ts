export type Suit = "hearts" | "diamonds" | "clubs" | "spades" | "HEARTS" | "DIAMONDS" | "CLUBS" | "SPADES";
export type Rank = "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" | "10" | "J" | "Q" | "K" | "A" | "TWO" | "THREE" | "FOUR" | "FIVE" | "SIX" | "SEVEN" | "EIGHT" | "NINE" | "TEN" | "JACK" | "QUEEN" | "KING" | "ACE";

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

// Backend types
export interface BackendCard {
  suit: string;
  rank: string;
}

export interface PlayerCardView {
  suit: string;
  rank: string;
}

export interface GameStateMessage {
  roomId: string;
  activePlayerId: string;
  currentCard: BackendCard;
  myCards: PlayerCardView[];
  playersCardsAmount: Record<string, number>;
  playersSkipTurns: Record<string, number>;
  specialEffectActive: boolean;
  demandedRank: string | null;
  demandedSuit: string | null;
  ranking: Record<string, number>;
  placement: Record<string, number>;
  losers: string[];
  status: "WAITING" | "PLAYING" | "FINISHED";
  drawDeckCardsAmount: number;
  discardDeckCardsAmount: number;
}

export interface PlayCardRequest {
  cardSuit: string;
  cardRank: string;
  demandedRank?: string;
  demandedSuit?: string;
}
