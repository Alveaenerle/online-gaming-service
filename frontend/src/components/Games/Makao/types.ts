// ============================================
// Makao Game Types - Backend API Types
// ============================================

// Suit type - supports both lowercase (display) and uppercase (backend)
export type Suit = "hearts" | "diamonds" | "clubs" | "spades" | "HEARTS" | "DIAMONDS" | "CLUBS" | "SPADES";

// Rank type - supports both display values and backend enums
export type Rank =
  | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" | "10" | "J" | "Q" | "K" | "A"
  | "TWO" | "THREE" | "FOUR" | "FIVE" | "SIX" | "SEVEN" | "EIGHT" | "NINE" | "TEN" | "JACK" | "QUEEN" | "KING" | "ACE";

/**
 * Basic card structure for display (used by Card.tsx)
 */
export interface Card {
  suit: Suit;
  rank: Rank;
  id: string;
}

/**
 * Card as received from backend
 */
export interface BackendCard {
  suit: string;
  rank: string;
}

/**
 * Player's card with playability info from backend
 */
export interface PlayerCardView {
  card: BackendCard;
  playable: boolean;
}

/**
 * Flattened card for internal use in components
 */
export interface MyCard {
  suit: string;
  rank: string;
  isPlayable: boolean;
}

/**
 * Game state message received from WebSocket
 */
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
  status: GameStatus;
  drawDeckCardsAmount: number;
  discardDeckCardsAmount: number;
}

/**
 * Local game state with transformed myCards
 */
export interface LocalGameState extends Omit<GameStateMessage, "myCards"> {
  myCards: MyCard[];
}

/**
 * Game status enum
 */
export type GameStatus = "WAITING" | "PLAYING" | "FINISHED";

/**
 * Request to play a card
 */
export interface PlayCardRequest {
  cardSuit: string;
  cardRank: string;
  requestRank?: string;
  requestSuit?: string;
}

/**
 * Response from drawing a card
 */
export interface DrawCardResponse {
  drawnCard: BackendCard | null;
  playable: boolean;
}

/**
 * Demand modal type (for Jack and Ace special cards)
 */
export type DemandType = "suit" | "rank";

/**
 * Demand modal state
 */
export interface DemandModalState {
  type: DemandType;
  card: MyCard;
}

/**
 * Drawn card state
 */
export interface DrawnCardState {
  suit: string;
  rank: string;
}
