import React from "react";
import { motion } from "framer-motion";
import Card from "./Card";
import { LocalGameState, MyCard, Suit, Rank } from "./types";
import { convertRank, convertSuit } from "./constants";

// ============================================
// Player Card Component - displays opponent info
// ============================================

interface PlayerCardProps {
  playerId: string;
  cardCount: number;
  isActive: boolean;
  skipTurns: number;
  playerName: string;
}

export const PlayerCard: React.FC<PlayerCardProps> = ({
  playerId,
  cardCount,
  isActive,
  skipTurns,
  playerName,
}) => {
  return (
    <div
      className={`bg-[#1a1a27] p-3 rounded-xl border transition-all ${
        isActive ? "border-purple-500 shadow-lg shadow-purple-500/20" : "border-white/10"
      }`}
    >
      <div className="flex items-center gap-2 mb-2">
        <div
          className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${
            isActive ? "bg-purple-600" : "bg-gray-600"
          }`}
        >
          {playerName.charAt(0).toUpperCase()}
        </div>
        <div>
          <p className="text-sm font-medium text-white">{playerName}</p>
          <p className="text-xs text-gray-400">{cardCount} kart</p>
        </div>
        {isActive && (
          <motion.div
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 1.5, repeat: Infinity }}
            className="w-2 h-2 rounded-full bg-green-500 ml-auto"
          />
        )}
      </div>
      {skipTurns > 0 && (
        <p className="text-xs text-red-400">Czeka: {skipTurns} tur</p>
      )}
      <div className="flex gap-0.5 mt-2">
        {Array.from({ length: Math.min(cardCount, 5) }).map((_, i) => (
          <div
            key={i}
            className="w-6 h-9 bg-gradient-to-br from-purple-600 to-purple-900 rounded border border-white/10"
          />
        ))}
        {cardCount > 5 && (
          <span className="text-xs text-gray-400 ml-1">+{cardCount - 5}</span>
        )}
      </div>
    </div>
  );
};

// ============================================
// Draw Pile Component
// ============================================

interface DrawPileProps {
  cardsCount: number;
  canDraw: boolean;
  onDraw: () => void;
}

export const DrawPile: React.FC<DrawPileProps> = ({ cardsCount, canDraw, onDraw }) => {
  return (
    <motion.div
      whileHover={canDraw ? { scale: 1.05 } : {}}
      className={`text-center ${canDraw ? "cursor-pointer" : "cursor-not-allowed opacity-70"}`}
      onClick={canDraw ? onDraw : undefined}
    >
      <div className="relative">
        <div className="absolute -top-0.5 -left-0.5 w-[62px] h-[87px] rounded-lg bg-purple-600/30" />
        <div className="absolute -top-1 -left-1 w-[62px] h-[87px] rounded-lg bg-purple-600/20" />
        <div className="w-[60px] h-[85px] rounded-lg bg-gradient-to-br from-purple-600 to-purple-900 border border-white/20 flex items-center justify-center">
          <span className="text-white/40 font-bold">OG</span>
        </div>
      </div>
      <p className="text-xs text-white/60 mt-2">Stos ({cardsCount})</p>
    </motion.div>
  );
};

// ============================================
// Discard Pile Component
// ============================================

interface DiscardPileProps {
  currentCard: { suit: string; rank: string } | null;
  cardsCount: number;
}

export const DiscardPile: React.FC<DiscardPileProps> = ({ currentCard, cardsCount }) => {
  if (!currentCard?.suit || !currentCard?.rank) {
    return (
      <div className="text-center">
        <div className="w-[72px] h-[101px] rounded-lg bg-gray-800 border border-gray-700 flex items-center justify-center">
          <span className="text-gray-600 text-xs">Pusty</span>
        </div>
        <p className="text-xs text-white/60 mt-2">Odrzucone ({cardsCount})</p>
      </div>
    );
  }

  return (
    <div className="text-center">
      <Card
        card={{
          suit: convertSuit(currentCard.suit) as Suit,
          rank: convertRank(currentCard.rank) as Rank,
          id: "discard",
        }}
        size="lg"
      />
      <p className="text-xs text-white/60 mt-2">Odrzucone ({cardsCount})</p>
    </div>
  );
};

// ============================================
// My Hand Component - displays player's cards
// ============================================

interface MyHandProps {
  cards: MyCard[];
  isMyTurn: boolean;
  specialEffectActive: boolean;
  username: string;
  onPlayCard: (card: MyCard) => void;
}

export const MyHand: React.FC<MyHandProps> = ({
  cards,
  isMyTurn,
  specialEffectActive,
  username,
  onPlayCard,
}) => {
  return (
    <div
      className={`p-4 rounded-2xl transition-all ${
        isMyTurn
          ? "bg-purple-600/20 border-2 border-purple-500"
          : "bg-white/5 border border-white/10"
      }`}
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-purple-700 flex items-center justify-center font-bold">
            {username?.charAt(0).toUpperCase() || "T"}
          </div>
          <div>
            <p className="text-sm font-medium">Ty</p>
            <p className="text-xs text-gray-400">{cards.length} kart</p>
          </div>
        </div>
        {isMyTurn && (
          <span className="px-3 py-1 bg-green-500/20 text-green-400 text-xs font-bold rounded-full animate-pulse">
            TWOJA TURA
          </span>
        )}
      </div>

      <div className="flex gap-2 flex-wrap justify-center">
        {cards.map((card, index) => {
          const isPlayable = isMyTurn && !specialEffectActive && card.isPlayable;
          return (
            <motion.div
              key={index}
              whileHover={isPlayable ? { y: -10, scale: 1.05 } : {}}
              className={isPlayable ? "cursor-pointer" : "cursor-not-allowed"}
              onClick={() => isPlayable && onPlayCard(card)}
            >
              <Card
                card={{
                  suit: convertSuit(card.suit) as Suit,
                  rank: convertRank(card.rank) as Rank,
                  id: `my-${index}`,
                }}
                size="md"
                isPlayable={isPlayable}
              />
            </motion.div>
          );
        })}
      </div>
    </div>
  );
};

// ============================================
// Game Table Component - main table layout
// ============================================

interface GameTableProps {
  gameState: LocalGameState;
  userId: string | undefined;
  isMyTurn: boolean;
  getPlayerName: (playerId: string) => string;
  onPlayCard: (card: MyCard) => void;
  onDrawCard: () => void;
  drawnCard: { suit: string; rank: string } | null;
}

export const GameTable: React.FC<GameTableProps> = ({
  gameState,
  userId,
  isMyTurn,
  getPlayerName,
  onPlayCard,
  onDrawCard,
  drawnCard,
}) => {
  // Get other players
  const otherPlayers = Object.entries(gameState.playersCardsAmount)
    .filter(([id]) => id !== userId);

  // Distribute players around table
  const distributePlayersAroundTable = () => {
    const total = otherPlayers.length;
    const top: typeof otherPlayers = [];
    const left: typeof otherPlayers = [];
    const right: typeof otherPlayers = [];

    if (total === 1) {
      top.push(otherPlayers[0]);
    } else if (total === 2) {
      left.push(otherPlayers[0]);
      right.push(otherPlayers[1]);
    } else if (total === 3) {
      left.push(otherPlayers[0]);
      top.push(otherPlayers[1]);
      right.push(otherPlayers[2]);
    } else if (total >= 4) {
      const perSide = Math.ceil(total / 3);
      otherPlayers.forEach((p, i) => {
        if (i < perSide) left.push(p);
        else if (i < perSide * 2) top.push(p);
        else right.push(p);
      });
    }

    return { top, left, right };
  };

  const { top: topPlayers, left: leftPlayers, right: rightPlayers } = distributePlayersAroundTable();

  const canDraw = isMyTurn && !gameState.specialEffectActive && !drawnCard;

  return (
    <div className="relative w-full max-w-5xl aspect-[16/10] bg-gradient-to-br from-[#18171f] to-[#0d0c12] rounded-[3rem] border border-purple-500/20 shadow-2xl shadow-black/50 p-6">
      {/* Table glow */}
      <div className="absolute inset-0 rounded-[3rem] bg-[radial-gradient(ellipse_at_center,_rgba(108,42,255,0.08),_transparent_60%)]" />

      {/* Top Players */}
      {topPlayers.length > 0 && (
        <div className="absolute top-4 left-1/2 -translate-x-1/2 flex gap-3">
          {topPlayers.map(([id, count]) => (
            <PlayerCard
              key={id}
              playerId={id}
              cardCount={count}
              isActive={id === gameState.activePlayerId}
              skipTurns={gameState.playersSkipTurns[id] || 0}
              playerName={getPlayerName(id)}
            />
          ))}
        </div>
      )}

      {/* Left Players */}
      {leftPlayers.length > 0 && (
        <div className="absolute left-4 top-1/2 -translate-y-1/2 flex flex-col gap-3">
          {leftPlayers.map(([id, count]) => (
            <PlayerCard
              key={id}
              playerId={id}
              cardCount={count}
              isActive={id === gameState.activePlayerId}
              skipTurns={gameState.playersSkipTurns[id] || 0}
              playerName={getPlayerName(id)}
            />
          ))}
        </div>
      )}

      {/* Right Players */}
      {rightPlayers.length > 0 && (
        <div className="absolute right-4 top-1/2 -translate-y-1/2 flex flex-col gap-3">
          {rightPlayers.map(([id, count]) => (
            <PlayerCard
              key={id}
              playerId={id}
              cardCount={count}
              isActive={id === gameState.activePlayerId}
              skipTurns={gameState.playersSkipTurns[id] || 0}
              playerName={getPlayerName(id)}
            />
          ))}
        </div>
      )}

      {/* Center - Deck & Discard */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 flex items-center gap-12">
        <DrawPile
          cardsCount={gameState.drawDeckCardsAmount}
          canDraw={canDraw}
          onDraw={onDrawCard}
        />
        <DiscardPile
          currentCard={gameState.currentCard}
          cardsCount={gameState.discardDeckCardsAmount}
        />
      </div>

      {/* Bottom - My Cards */}
      <div className="absolute bottom-4 left-1/2 -translate-x-1/2 max-w-[85%]">
        <MyHand
          cards={gameState.myCards}
          isMyTurn={isMyTurn}
          specialEffectActive={gameState.specialEffectActive}
          username={userId || ""}
          onPlayCard={onPlayCard}
        />
      </div>
    </div>
  );
};
