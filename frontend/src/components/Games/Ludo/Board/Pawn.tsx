import { motion, useAnimation } from "framer-motion";
import { getPawnCoords, getPathCoords } from "./ludoLogic";
import { Color } from "./constants";
import { LudoPawn } from "../types";
import { useState, useEffect, useRef } from "react";

interface PawnProps {
  pawn: LudoPawn;
  isInteractable?: boolean;
  diceValue?: number;
  onMoveComplete?: (pawnIndex: number) => void;
  onClick?: (pawnIndex: number) => void;
}

export function Pawn({
  pawn,
  isInteractable,
  diceValue,
  onMoveComplete,
  onClick,
}: PawnProps) {
  const controls = useAnimation();
  const [isMoving, setIsMoving] = useState(false);

  // Ref do przechowywania poprzedniej pozycji, aby wiedzieć czy odpalić animację
  const prevPositionRef = useRef(pawn.position);

  const { id, position, color } = pawn;
  const { row, col } = getPawnCoords(position, color as Color, id);

  // --- LOGIKA DEBUGOWANIA I ANIMACJI ---

  useEffect(() => {
    const oldPos = prevPositionRef.current;

    // Jeśli pozycja w propsach się zmieniła (np. po aktualizacji z WebSocketu)
    if (oldPos !== position) {
      console.log(
        `[Pawn-${color}-${id}] Position changed: ${oldPos} -> ${position}`
      );
      prevPositionRef.current = position;

      // Wyzwalaj animację tylko jeśli pionek porusza się do przodu (nie jest zbity)
      // Jeśli position === -1, oznacza to zbicie - teleportujemy pionka do bazy
      if (position !== -1 && oldPos !== position) {
        // Obliczamy dystans (kroki). Jeśli mamy diceValue z propsów, używamy go.
        // Jeśli nie (np. u przeciwnika), obliczamy różnicę pozycji.
        const steps = position - oldPos;

        handleAnimateSequence(oldPos, steps);
      } else {
        // Natychmiastowa synchronizacja pozycji (np. przy zbiciu)
        controls.set({ gridRow: row, gridColumn: col });
      }
    }
  }, [position]); // Reaguje u każdego gracza, gdy przyjdzie nowy stan

  const handleAnimateSequence = async (startPos: number, steps: number) => {
    if (steps <= 0 || isMoving) return;

    console.log(
      `[Pawn-${color}-${id}] Starting animation sequence for ${steps} steps`
    );
    setIsMoving(true);

    const path = getPathCoords(startPos, steps, color as Color, id);

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
    console.log(`[Pawn-${color}-${id}] Animation finished`);

    if (onMoveComplete) {
      onMoveComplete(id);
    }
  };

  const handleLocalClick = () => {
    if (!isInteractable || isMoving) return;

    console.log(`[Pawn-${color}-${id}] Clicked by user`);
    if (onClick) onClick(id);
  };

  // --- RENDERING (BEZ ZMIAN W STYLACH) ---

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
      onClick={handleLocalClick}
      whileHover={isInteractable && !isMoving ? { scale: 1.1 } : {}}
      className={`
        relative w-[85%] h-[85%] rounded-full m-auto
        border-2 border-white/60 shadow-xl flex items-center justify-center
        ${
          isMoving
            ? "cursor-default"
            : isInteractable
            ? "cursor-pointer"
            : "cursor-default"
        }
        ${colorConfig[color as Color]}
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
          layoutId={`pulse-${color}-${id}`}
          className="absolute -inset-2 rounded-full border-2 border-white/50"
          animate={{ scale: [1, 1.3], opacity: [0.5, 0] }}
          transition={{ repeat: Infinity, duration: 1 }}
        />
      )}

      {isInteractable && diceValue && !isMoving && (
        <motion.div
          initial={{ opacity: 0, scale: 0.5 }}
          animate={{ opacity: 1, scale: 1, y: -30 }}
          className="absolute whitespace-nowrap bg-white text-slate-900 text-[10px] px-2 py-0.5 rounded-md font-black shadow-xl border border-white"
        >
          {diceValue}
        </motion.div>
      )}
    </motion.div>
  );
}
