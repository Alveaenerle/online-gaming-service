// ============================================
// Backend-aligned types for Makao game
// ============================================

// Enums matching backend exactly
export type CardSuit = "HEARTS" | "DIAMONDS" | "CLUBS" | "SPADES";
export type CardRank =
  | "TWO"
  | "THREE"
  | "FOUR"
  | "FIVE"
  | "SIX"
  | "SEVEN"
  | "EIGHT"
  | "NINE"
  | "TEN"
  | "JACK"
  | "QUEEN"
  | "KING"
  | "ACE";

export type RoomStatus = "WAITING" | "PLAYING" | "FINISHED";

// Card from backend
export interface Card {
  suit: CardSuit;
  rank: CardRank;
}

// Player card view with playability info
export interface PlayerCardView {
  card: Card;
  playable: boolean;
}

// Game state message from WebSocket
export interface GameStateMessage {
  roomId: string;
  activePlayerId: string;
  currentCard: Card;
  myCards: PlayerCardView[];
  playersCardsAmount: Record<string, number>;
  playersSkipTurns: Record<string, number>;
  specialEffectActive: boolean;
  demandedRank: CardRank | null;
  demandedSuit: CardSuit | null;
  ranking: Record<string, number>;
  placement: Record<string, number>;
  losers: string[];
  status: RoomStatus;
  drawDeckCardsAmount: number;
  discardDeckCardsAmount: number;
  playersUsernames?: Record<string, string>;
}

// Request to play a card
export interface PlayCardRequest {
  cardSuit: CardSuit;
  cardRank: CardRank;
  requestSuit?: CardSuit | null;
  requestRank?: CardRank | null;
}

// Response from draw card endpoint
export interface DrawCardResponse {
  drawnCard: Card;
  playable: boolean;
}

// Player view for UI rendering
export interface PlayerView {
  id: string;
  username: string;
  cardCount: number;
  isActive: boolean;
  isMe: boolean;
  placement: number | null;
  skipTurns: number;
}

// Demand types for special cards
export type DemandType = "suit" | "rank";

// Demandable ranks (for Jack)
export const DEMANDABLE_RANKS: CardRank[] = [
  "FIVE",
  "SIX",
  "SEVEN",
  "EIGHT",
  "NINE",
  "TEN",
];

// All suits (for Ace)
export const ALL_SUITS: CardSuit[] = ["HEARTS", "DIAMONDS", "CLUBS", "SPADES"];
