import type {
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
 * Get avatar URL from avatarId
 * Handles both human and bot avatars
 */
export const getAvatarUrl = (avatarId: string | undefined, playerId: string): string => {
  // Bot avatar
  if (playerId.startsWith("bot-") || avatarId === "bot_avatar.png") {
    return "/avatars/bot_avatar.svg";
  }
  // Human avatar - construct URL from avatarId
  if (avatarId) {
    return `/avatars/${avatarId}`;
  }
  // Default fallback avatar
  return "/avatars/avatar_1.png";
};

/**
 * Build player views from game state
 * Uses playerOrder from backend to maintain correct turn order
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
    playersAvatars,
    playerOrder,
  } = gameState;

  // Use playerOrder if available, otherwise fall back to playersCardsAmount keys
  const playerIds = playerOrder && playerOrder.length > 0
    ? playerOrder
    : Object.keys(playersCardsAmount);

  return playerIds.map((playerId) => ({
    id: playerId,
    username: playersUsernames?.[playerId] || getPlayerDisplayName(playerId),
    avatarUrl: getAvatarUrl(playersAvatars?.[playerId], playerId),
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

export type Position = "bottom" | "left" | "top" | "right" | "top-left" | "top-right" | "bottom-left" | "bottom-right";

interface PositionedPlayer extends PlayerView {
  position: Position;
}

/**
 * Position configurations for different player counts (2-8 players)
 * Positions are assigned clockwise starting from the position after "bottom" (local player)
 *
 * Layout visualization:
 *       top-left    top    top-right
 *          |         |         |
 *   left --+----[TABLE]----+-- right
 *          |         |         |
 *  bottom-left  [BOTTOM]  bottom-right
 *              (local user)
 */
const POSITION_LAYOUTS: Record<number, Position[]> = {
  // 2 players: Me at bottom, opponent at top
  2: ["top"],
  // 3 players: Me at bottom, others at left and right
  3: ["left", "right"],
  // 4 players: Me at bottom, others distributed evenly
  4: ["left", "top", "right"],
  // 5 players
  5: ["left", "top-left", "top-right", "right"],
  // 6 players
  6: ["left", "top-left", "top", "top-right", "right"],
  // 7 players
  7: ["bottom-left", "left", "top-left", "top-right", "right", "bottom-right"],
  // 8 players - full layout
  8: ["bottom-left", "left", "top-left", "top", "top-right", "right", "bottom-right"],
};

/**
 * Get positions for other players based on total player count
 */
const getPositionsForPlayerCount = (otherPlayersCount: number): Position[] => {
  // Total players = otherPlayersCount + 1 (the local user)
  const totalPlayers = otherPlayersCount + 1;

  // Clamp to supported range (2-8)
  const clampedTotal = Math.max(2, Math.min(8, totalPlayers));

  const layout = POSITION_LAYOUTS[clampedTotal];

  // If we have fewer "others" than positions in the layout, use first N positions
  // If we have more, we'll cycle through (edge case for 8+ players)
  return layout.slice(0, otherPlayersCount);
};

/**
 * Rotate an array so that the element at the given index becomes the first element
 * Used to make the local user's position the starting point
 */
const rotateArray = <T>(arr: T[], startIndex: number): T[] => {
  if (arr.length === 0 || startIndex === 0) return [...arr];
  const normalizedIndex = ((startIndex % arr.length) + arr.length) % arr.length;
  return [...arr.slice(normalizedIndex), ...arr.slice(0, normalizedIndex)];
};

/**
 * Distribute players around the table relative to current player
 *
 * This function:
 * 1. Takes the playerOrder list from the backend (maintains turn order)
 * 2. Rotates the list so the local user is at index 0 (bottom position)
 * 3. Assigns positions to other players going clockwise from left
 *
 * The local user is always at "bottom" position, and other players
 * are distributed clockwise starting from "left" based on player count.
 */
export const distributePlayersAroundTable = (
  players: PlayerView[],
  myUserId: string
): { me: PlayerView | null; others: PositionedPlayer[] } => {
  if (players.length === 0) {
    return { me: null, others: [] };
  }

  // Find the local user's index in the player order
  const myIndex = players.findIndex((p) => p.id === myUserId);

  if (myIndex === -1) {
    // User not found in players - return first player as "me" fallback
    const me = players[0] || null;
    const others = players.slice(1);
    const positions = getPositionsForPlayerCount(others.length);

    const positionedOthers: PositionedPlayer[] = others.map((player, index) => ({
      ...player,
      position: positions[index] || "top",
    }));

    return { me, others: positionedOthers };
  }

  // Rotate the array so the local user is at index 0
  const rotatedPlayers = rotateArray(players, myIndex);

  // Local user is now at index 0
  const me = rotatedPlayers[0];

  // Other players are indices 1 to n (in clockwise turn order)
  const others = rotatedPlayers.slice(1);

  // Get position assignments based on how many other players there are
  const positions = getPositionsForPlayerCount(others.length);

  // Assign positions to other players
  // Players are in turn order, positions go clockwise from left
  const positionedOthers: PositionedPlayer[] = others.map((player, index) => ({
    ...player,
    position: positions[index] || "top",
  }));

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
