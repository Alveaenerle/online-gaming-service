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
  sm: { width: 48, height: 67 },
  md: { width: 58, height: 82 },
  lg: { width: 72, height: 101 },
};

const getRankName = (rank: string): string => {
  switch (rank) {
    case "A": return "ace";
    case "K": return "king";
    case "Q": return "queen";
    case "J": return "jack";
    default: return rank;
  }
};

const Card: React.FC<CardProps> = ({
  card,
  onClick,
  isPlayable = false,
  isFaceDown = false,
  size = "md",
}) => {
  const s = SIZES[size];

  if (isFaceDown) {
    return (
      <motion.div
        whileHover={onClick ? { scale: 1.05, y: -2 } : {}}
        style={{ width: s.width, height: s.height }}
        className={`rounded-lg bg-gradient-to-br from-purpleStart to-purpleEnd shadow-md flex items-center justify-center ${onClick ? "cursor-pointer" : ""}`}
        onClick={onClick}
      >
        <span className="text-white/40 font-bold text-base">OG</span>
      </motion.div>
    );
  }

  const rankName = getRankName(card.rank);
  const cardImage = `/SVG-cards-1.3/${rankName}_of_${card.suit}.svg`;

  return (
    <motion.div
      whileHover={isPlayable ? { scale: 1.08, y: -8 } : {}}
      onClick={isPlayable ? onClick : undefined}
      style={{ width: s.width, height: s.height }}
      className={`rounded-lg overflow-hidden shadow-md ${
        isPlayable
          ? "ring-2 ring-purpleEnd cursor-pointer shadow-neon"
          : "opacity-80"
      }`}
    >
      <img
        src={cardImage}
        alt={`${card.rank} of ${card.suit}`}
        className="w-full h-full object-contain bg-white"
        draggable={false}
      />
    </motion.div>
  );
};

export default Card;
