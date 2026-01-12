import { motion } from "framer-motion";
import { Card as CardType } from "./types";
import { getCardImagePath, RANK_DISPLAY, isSpecialCard } from "./utils/cardHelpers";

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
  const special = isSpecialCard(card);

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

  const cardImage = getCardImagePath(card);

  return (
    <motion.div
      whileHover={isPlayable ? { scale: 1.08, y: -8 } : {}}
      onClick={isPlayable ? onClick : undefined}
      style={{ width: s.width, height: s.height }}
      className={`rounded-lg overflow-hidden shadow-md relative transition-all duration-200 ${
        isPlayable
          ? "cursor-pointer shadow-lg hover:shadow-xl"
          : "opacity-90 grayscale-[0.2]"
      } ${special ? "border border-white/10" : ""}`}
    >
      <img
        src={cardImage}
        alt={`${RANK_DISPLAY[card.rank]} of ${card.suit}`}
        className="w-full h-full object-contain bg-white"
        draggable={false}
      />
    </motion.div>
  );
};

export default Card;
