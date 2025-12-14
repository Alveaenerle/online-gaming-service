import React from "react";
import { motion } from "framer-motion";
import { Player as PlayerType, Card as CardType } from "./types";
import Card from "./Card";

interface PlayerProps {
  player: PlayerType;
  isCurrentPlayer: boolean;
  onCardClick?: (card: CardType) => void;
  canPlayCard?: (card: CardType) => boolean;
  compact?: boolean;
  position?: "top" | "left" | "right" | "bottom";
}

const Player: React.FC<PlayerProps> = ({
  player,
  isCurrentPlayer,
  onCardClick,
  canPlayCard,
  compact = false,
  position = "bottom",
}) => {
  const isHorizontal = position === "top" || position === "bottom";

  return (
    <div
      className={`flex ${isHorizontal ? "flex-col" : "flex-row"} items-center gap-2 p-2 rounded-xl backdrop-blur-sm ${
        isCurrentPlayer
          ? "bg-purpleEnd/20 border border-purpleEnd/50 shadow-lg shadow-purpleEnd/20"
          : "bg-white/5 border border-white/10"
      }`}
    >
      {/* Player Info */}
      <div className="flex items-center gap-2">
        <div
          className={`w-7 h-7 rounded-full flex items-center justify-center font-bold text-xs ${
            player.isHuman
              ? "bg-gradient-to-br from-purpleStart to-purpleEnd text-white"
              : "bg-gray-600 text-gray-200"
          }`}
        >
          {player.name.charAt(0).toUpperCase()}
        </div>
        <div className="text-left">
          <p className="text-white text-xs font-medium leading-tight">{player.name}</p>
          <p className="text-gray-400 text-[10px]">{player.cards.length} cards</p>
        </div>
        {isCurrentPlayer && (
          <motion.div
            animate={{ opacity: [0.5, 1, 0.5] }}
            transition={{ duration: 1.5, repeat: Infinity }}
            className="w-2 h-2 rounded-full bg-purpleEnd"
          />
        )}
      </div>

      {/* Cards */}
      <div className={`flex ${isHorizontal ? "flex-row" : "flex-col"} gap-1 flex-wrap justify-center`}>
        {player.isHuman ? (
          player.cards.map((card) => {
            const playable = canPlayCard ? canPlayCard(card) : false;
            return (
              <Card
                key={card.id}
                card={card}
                isPlayable={isCurrentPlayer && playable}
                onClick={() => isCurrentPlayer && playable && onCardClick?.(card)}
                size="md"
              />
            );
          })
        ) : compact ? (
          <div className="flex items-center gap-1.5">
            {player.cards.length > 0 && (
              <Card card={player.cards[0]} isFaceDown size="sm" />
            )}
            <span className="text-white/80 font-semibold text-sm">×{player.cards.length}</span>
          </div>
        ) : (
          player.cards.map((card) => (
            <Card key={card.id} card={card} isFaceDown size="sm" />
          ))
        )}
      </div>
    </div>
  );
};

export default Player;