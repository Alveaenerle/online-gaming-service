import { motion } from "framer-motion";
import { getPawnCoords } from "./ludoLogic";
import { Color } from "./constants";

interface PawnProps {
  color: Color;
  position: number;
  pawnIndex: number;
  isInteractable?: boolean;
  onClick?: () => void;
}

export function Pawn({
  color,
  position,
  pawnIndex,
  isInteractable,
  onClick,
}: PawnProps) {
  const { row, col } = getPawnCoords(position, color, pawnIndex);

  const colorConfig = {
    RED: "bg-red-500 shadow-red-500/50",
    BLUE: "bg-blue-500 shadow-blue-500/50",
    YELLOW: "bg-yellow-500 shadow-yellow-500/50",
    GREEN: "bg-green-500 shadow-green-500/50",
  };

  return (
    <motion.div
      layout
      transition={{ type: "spring", stiffness: 300, damping: 25 }}
      style={{ gridRow: row, gridColumn: col }}
      onClick={onClick}
      className={`
        relative z-20 w-full h-full rounded-full cursor-pointer
        border-2 border-white/40 shadow-lg
        flex items-center justify-center
        ${colorConfig[color]}
        ${
          isInteractable
            ? "hover:scale-110 ring-2 ring-white animate-pulse"
            : ""
        }
      `}
    >
      <div className="absolute top-1 left-1 w-1/2 h-1/2 bg-white/20 rounded-full blur-[1px]" />
    </motion.div>
  );
}
