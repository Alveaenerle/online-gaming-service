import {
  Card,
  CardSuit,
  CardRank,
  GameStateMessage,
  PlayerView,
} from "../types";

// ============================================
// Display mappings
// ============================================

export const RANK_DISPLAY: Record<CardRank, string> = {
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

export const SUIT_INFO: Record<
  CardSuit,
  { symbol: string; color: string; name: string }
> = {
  HEARTS: { symbol: "♥", color: "text-red-500", name: "hearts" },
  DIAMONDS: { symbol: "♦", color: "text-red-500", name: "diamonds" },
  CLUBS: { symbol: "♣", color: "text-gray-900", name: "clubs" },
  SPADES: { symbol: "♠", color: "text-gray-900", name: "spades" },
};

// ============================================
// Card image helpers
// ============================================

/**
 * Get the file name for a rank (for SVG-cards naming convention)
 */
const getRankFileName = (rank: CardRank): string => {
  const rankMap: Record<CardRank, string> = {
    TWO: "2",
    THREE: "3",
    FOUR: "4",
    FIVE: "5",
    SIX: "6",
    SEVEN: "7",
    EIGHT: "8",
    NINE: "9",
    TEN: "10",
    JACK: "jack",
    QUEEN: "queen",
    KING: "king",
    ACE: "ace",
  };
  return rankMap[rank];
};

/**
 * Get the path to the card image
 */
export const getCardImagePath = (card: Card): string => {
  const rankName = getRankFileName(card.rank);
  const suitName = SUIT_INFO[card.suit].name;
  return `/SVG-cards-1.3/${rankName}_of_${suitName}.svg`;
};

/**
 * Get the path to card back image
 */
export const getCardBackPath = (): string => {
  return "/SVG-cards-1.3/back.svg";
};

// ============================================
// Special card helpers
// ============================================

/**
 * Check if card is a special card (has game effect)
 */
export const isSpecialCard = (card: Card): boolean => {
  const specialRanks: CardRank[] = [
    "TWO",
    "THREE",
    "FOUR",
    "JACK",
    "KING",
    "ACE",
  ];
  return specialRanks.includes(card.rank);
};

/**
 * Check if card requires demand selection
 */
export const requiresDemand = (card: Card): "suit" | "rank" | null => {
  if (card.rank === "ACE") return "suit";
  if (card.rank === "JACK") return "rank";
  return null;
};

/**
 * Check if King changes direction (hearts or spades)
 */
export const isDirectionChangingKing = (card: Card): boolean => {
  return (
    card.rank === "KING" &&
    (card.suit === "HEARTS" || card.suit === "SPADES")
  );
};

/**
 * Get effect description for a card
 */
export const getCardEffectDescription = (card: Card): string | null => {
  switch (card.rank) {
    case "TWO":
      return "Next player draws +2 cards";
    case "THREE":
      return "Next player draws +3 cards";
    case "FOUR":
      return "Next player skips turn";
    case "JACK":
      return "Demand a rank (5-10)";
    case "ACE":
      return "Demand a suit";
    case "KING":
      if (card.suit === "HEARTS" || card.suit === "SPADES") {
        return "Reverses play direction";
      }
      return null;
    default:
      return null;
  }
};

// ============================================
// Player helpers
// ============================================

/**
 * Build player views from game state
 */
export const buildPlayerViews = (
  gameState: GameStateMessage,
  myUserId: string
): PlayerView[] => {
  const {
    playersCardsAmount,
    playersSkipTurns,
    activePlayerId,
    placement,
    playersUsernames,
  } = gameState;

  const playerIds = Object.keys(playersCardsAmount);

  return playerIds.map((playerId) => ({
    id: playerId,
    username: playersUsernames?.[playerId] || getPlayerDisplayName(playerId),
    cardCount: playersCardsAmount[playerId] || 0,
    isActive: playerId === activePlayerId,
    isMe: playerId === myUserId,
    placement: placement[playerId] || null,
    skipTurns: playersSkipTurns[playerId] || 0,
  }));
};

/**
 * Get display name for a player (handle bots)
 */
export const getPlayerDisplayName = (playerId: string): string => {
  if (playerId.startsWith("bot-")) {
    const botNumber = playerId.replace("bot-", "").slice(0, 4);
    return `Bot ${botNumber}`;
  }
  return playerId.slice(0, 8);
};

/**
 * Check if player is a bot
 */
export const isBot = (playerId: string): boolean => {
  return playerId.startsWith("bot-");
};

// ============================================
// UI Positioning helpers
// ============================================

type Position = "bottom" | "left" | "top" | "right";

interface PositionedPlayer extends PlayerView {
  position: Position;
}

/**
 * Distribute players around the table relative to current player
 */
export const distributePlayersAroundTable = (
  players: PlayerView[],
  myUserId: string
): { me: PlayerView | null; others: PositionedPlayer[] } => {
  const me = players.find((p) => p.isMe) || null;
  const others = players.filter((p) => !p.isMe);

  const total = others.length;

  if (total === 0) {
    return { me, others: [] };
  }

  // Position assignments based on player count
  const positionedOthers: PositionedPlayer[] = others.map((player, index) => {
    let position: Position;

    if (total === 1) {
      position = "top";
    } else if (total === 2) {
      position = index === 0 ? "left" : "right";
    } else if (total === 3) {
      position = ["left", "top", "right"][index] as Position;
    } else {
      // 4+ players - distribute evenly
      const positions: Position[] = ["left", "top", "top", "right"];
      if (total <= 4) {
        position = positions[index] || "top";
      } else {
        // More complex distribution for 5+ players
        if (index < total / 3) position = "left";
        else if (index < (2 * total) / 3) position = "top";
        else position = "right";
      }
    }

    return { ...player, position };
  });

  return { me, others: positionedOthers };
};

// ============================================
// Unique key generators
// ============================================

/**
 * Generate unique key for a card
 */
export const getCardKey = (card: Card, index: number): string => {
  return `${card.suit}-${card.rank}-${index}`;
};
