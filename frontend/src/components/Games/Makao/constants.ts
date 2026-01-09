// Backend enum mappings

/**
 * Maps backend rank enums to display values
 */
export const RANK_MAP: Record<string, string> = {
  TWO: "2",
  THREE: "3",
  FOUR: "4",
  FIVE: "5",
  SIX: "6",
  SEVEN: "7",
  EIGHT: "8",
  NINE: "9",
  TEN: "10",
  JACK: "J",
  QUEEN: "Q",
  KING: "K",
  ACE: "A",
};

/**
 * Maps display rank values back to backend enums
 */
export const RANK_TO_BACKEND: Record<string, string> = {
  "2": "TWO",
  "3": "THREE",
  "4": "FOUR",
  "5": "FIVE",
  "6": "SIX",
  "7": "SEVEN",
  "8": "EIGHT",
  "9": "NINE",
  "10": "TEN",
  J: "JACK",
  Q: "QUEEN",
  K: "KING",
  A: "ACE",
};

/**
 * All available suits for Ace demand
 */
export const SUITS = ["HEARTS", "DIAMONDS", "CLUBS", "SPADES"] as const;

/**
 * Available ranks for Jack demand (5-10 only in Makao rules)
 */
export const DEMANDABLE_RANKS = ["FIVE", "SIX", "SEVEN", "EIGHT", "NINE", "TEN"] as const;

/**
 * Suit symbols for display
 */
export const SUIT_SYMBOLS: Record<string, string> = {
  HEARTS: "♥",
  DIAMONDS: "♦",
  CLUBS: "♣",
  SPADES: "♠",
};

/**
 * Maps suit to color class
 */
export const SUIT_COLORS: Record<string, string> = {
  HEARTS: "text-red-500",
  DIAMONDS: "text-red-500",
  CLUBS: "text-white",
  SPADES: "text-white",
};

/**
 * Convert backend rank to display rank
 */
export const convertRank = (rank: string): string => RANK_MAP[rank] || rank;

/**
 * Convert backend suit to lowercase for SVG paths
 */
export const convertSuit = (suit: string): string => suit.toLowerCase();

/**
 * Convert display rank to backend rank
 */
export const toBackendRank = (rank: string): string => RANK_TO_BACKEND[rank] || rank.toUpperCase();
