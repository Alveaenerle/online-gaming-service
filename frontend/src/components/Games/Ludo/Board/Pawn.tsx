import { motion, useAnimation } from "framer-motion";
import { getPawnCoords, getPathCoords } from "./ludoLogic";
import { Color } from "./constants";
import { useCallback, useState } from "react";

interface PawnProps {
  color: Color;
  position: number;
  pawnIndex: number;
  isInteractable?: boolean;
  diceValue?: number;
  onMoveComplete?: (pawnIndex: number) => void;
}

export function Pawn({
  color,
  position,
  pawnIndex,
  isInteractable,
  diceValue,
  onMoveComplete,
}: PawnProps) {
  const controls = useAnimation();
  const [isMoving, setIsMoving] = useState(false);
  const { row, col } = getPawnCoords(position, color, pawnIndex);

  const handleAnimateMove = async () => {
    if (!isInteractable || !diceValue || diceValue <= 0 || isMoving) return;

    setIsMoving(true);

    const path = getPathCoords(position, diceValue, color, pawnIndex);

    for (const step of path) {
      await controls.start({
        gridRow: step.row,
        gridColumn: step.col,
        y: [0, -25, 0],
        scaleX: [1, 0.8, 1.2, 1],
        scaleY: [1, 1.4, 0.8, 1],
        transition: {
          duration: 0.5,
          times: [0, 0.4, 0.8, 1],
          ease: "easeInOut",
        },
      });
    }

    setIsMoving(false);

    if (onMoveComplete) {
      onMoveComplete(pawnIndex);
    }
  };

  const colorConfig = {
    RED: "bg-red-500 shadow-red-500/50 ring-red-400",
    BLUE: "bg-blue-500 shadow-blue-500/50 ring-blue-400",
    YELLOW: "bg-yellow-500 shadow-yellow-500/50 ring-yellow-400",
    GREEN: "bg-green-500 shadow-green-500/50 ring-green-400",
  };

  return (
    <motion.div
      animate={controls}
      initial={false}
      style={{
        gridRow: row,
        gridColumn: col,
        zIndex: isMoving ? 50 : isInteractable ? 30 : 20,
      }}
      onClick={handleAnimateMove}
      whileHover={isInteractable && !isMoving ? { scale: 1.1 } : {}}
      className={`
        relative w-[85%] h-[85%] rounded-full m-auto
        border-2 border-white/60 shadow-xl
        flex items-center justify-center
        ${isMoving ? "cursor-default" : "cursor-pointer"}
        ${colorConfig[color]}
        ${
          isInteractable && !isMoving
            ? "ring-4 ring-white shadow-[0_0_20px_white]"
            : "opacity-90 grayscale-[0.1]"
        }
      `}
    >
      <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-black/20 via-transparent to-white/30" />
      <div className="absolute top-1 left-1 w-1/2 h-1/2 bg-white/30 rounded-full blur-[1px]" />

      {isInteractable && !isMoving && (
        <motion.div
          layoutId={`pulse-${color}-${pawnIndex}`}
          className="absolute -inset-2 rounded-full border-2 border-white/50"
          animate={{ scale: [1, 1.3], opacity: [0.5, 0] }}
          transition={{ repeat: Infinity, duration: 1 }}
        />
      )}

      {isInteractable && diceValue && !isMoving && (
        <motion.div
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: 1, scale: 1, y: -30 }}
          exit={{ opacity: 0, scale: 0 }}
          className="absolute whitespace-nowrap bg-white text-slate-900 text-[10px] px-2 py-0.5 rounded-md font-black shadow-xl border border-white"
        >
          {diceValue}
        </motion.div>
      )}
    </motion.div>
  );
}
