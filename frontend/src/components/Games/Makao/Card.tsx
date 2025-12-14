import React from "react";
import { motion } from "framer-motion";
import { Card as CardType } from "./types";

interface CardProps {
  card: CardType;
  onClick?: () => void;
  isPlayable?: boolean;
  isFaceDown?: boolean;
  size?: "sm" | "md" | "lg";
}

const SIZES = {
  sm: { width: "w-10", height: "h-14", rank: "text-[8px]", suit: "text-[9px]", center: "text-base" },
  md: { width: "w-12", height: "h-[4.25rem]", rank: "text-[9px]", suit: "text-[10px]", center: "text-lg" },
  lg: { width: "w-14", height: "h-[5rem]", rank: "text-[10px]", suit: "text-xs", center: "text-xl" },
};

const SUIT_SYMBOLS: Record<string, string> = {
  hearts: "♥",
  diamonds: "♦",
  clubs: "♣",
  spades: "♠",
};

const Card: React.FC<CardProps> = ({
  card,
  onClick,
  isPlayable = false,
  isFaceDown = false,
  size = "md",
}) => {
  const s = SIZES[size];
  const isRed = card.suit === "hearts" || card.suit === "diamonds";
  const suitColor = isRed ? "text-red-500" : "text-gray-800";

  if (isFaceDown) {
    return (
      <motion.div
        whileHover={onClick ? { scale: 1.05, y: -2 } : {}}
        className={`${s.width} ${s.height} rounded-md bg-gradient-to-br from-purpleStart to-purpleEnd shadow-md flex items-center justify-center ${onClick ? "cursor-pointer" : ""}`}
        onClick={onClick}
      >
        <span className="text-white/40 font-bold text-sm">OG</span>
      </motion.div>
    );
  }

  return (
    <motion.div
      whileHover={isPlayable ? { scale: 1.08, y: -8 } : {}}
      onClick={isPlayable ? onClick : undefined}
      className={`${s.width} ${s.height} rounded-md bg-white shadow-md flex flex-col justify-between p-0.5 ${
        isPlayable
          ? "border-2 border-purpleEnd cursor-pointer shadow-neon"
          : "border border-gray-200 opacity-75"
      }`}
    >
      {/* Top */}
      <div className={`${suitColor} font-bold leading-none`}>
        <div className={s.rank}>{card.rank}</div>
        <div className={s.suit}>{SUIT_SYMBOLS[card.suit]}</div>
      </div>

      {/* Center */}
      <div className={`${suitColor} ${s.center} text-center -my-1`}>
        {SUIT_SYMBOLS[card.suit]}
      </div>

      {/* Bottom */}
      <div className={`${suitColor} font-bold leading-none self-end rotate-180`}>
        <div className={s.rank}>{card.rank}</div>
        <div className={s.suit}>{SUIT_SYMBOLS[card.suit]}</div>
      </div>
    </motion.div>
  );
};

export default Card;
