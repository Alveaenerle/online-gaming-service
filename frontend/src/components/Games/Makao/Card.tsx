import { motion } from "framer-motion";
import type { Card as CardType } from "./types";
import { getCardImagePath, RANK_DISPLAY } from "./utils/cardHelpers";

interface CardProps {
  card: CardType;
  onClick?: () => void;
  isPlayable?: boolean;
  isFaceDown?: boolean;
  size?: "sm" | "md" | "lg";
  showEffect?: boolean;
}

const SIZES = {
  sm: { width: 48, height: 67 },
  md: { width: 58, height: 82 },
  lg: { width: 72, height: 101 },
};

const Card: React.FC<CardProps> = ({
  card,
  onClick,
  isPlayable = false,
  isFaceDown = false,
  size = "md",
  showEffect = false,
}) => {
  const s = SIZES[size];

  if (isFaceDown) {
    return (
      <motion.div
        whileHover={onClick ? { scale: 1.05, y: -2 } : {}}
        style={{ width: s.width, height: s.height }}
        className={`rounded-s bg-gradient-to-br from-purpleStart to-purpleEnd shadow-md flex items-center justify-center ${onClick ? "cursor-pointer" : ""}`}
        onClick={onClick}
      >
        <span className="text-white/40 font-bold text-base">OG</span>
      </motion.div>
    );
  }

  const cardImage = getCardImagePath(card);

  return (
    <motion.div
      whileHover={isPlayable ? { scale: 1.08, y: -8 } : {}}
      onClick={isPlayable ? onClick : undefined}
      style={{ width: s.width, height: s.height }}
      className={`
        relative rounded-s overflow-hidden  border-white shadow-xl
        flex items-center justify-center transition-all duration-200
        ${isPlayable
          ? "cursor-pointer hover:shadow-2xl hover:border-white"
          : "opacity-90 grayscale-[0.2] border-gray-100"
        }
      `}
    >
      <img
        src={cardImage}
        alt={`${RANK_DISPLAY[card.rank]} of ${card.suit}`}
        className="w-full h-full object-contain pointer-events-none"
        draggable={false}
      />

      {/* Selection/Effect overlay */}
      {showEffect && (
        <div className="absolute inset-0 bg-purpleEnd/10 mix-blend-overlay pointer-events-none" />
      )}
    </motion.div>
  );
};

export default Card;
