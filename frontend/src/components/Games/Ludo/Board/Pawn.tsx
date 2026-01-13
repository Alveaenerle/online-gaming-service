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
  isInteractable: isPlayerTurn,
  diceValue,
  onMoveComplete,
  onClick,
}: PawnProps) {
  const controls = useAnimation();
  const [isMoving, setIsMoving] = useState(false);
  const { id, position, color, stepsMoved } = pawn;

  // Logiczne współrzędne
  const { row, col } = getPawnCoords(position, stepsMoved, color as Color, id);

  // Wizualne współrzędne - zainicjalizowane aktualną pozycją
  const [visualCoords, setVisualCoords] = useState({ row, col });
  const prevPositionRef = useRef(position);

  // Definicja zmiennych interakcji (przed useEffectami!)
  const canMoveThisPawn =
    (position !== -1 || diceValue === 6) && position !== -2;
  const activeInteractable = !!(isPlayerTurn && canMoveThisPawn && !isMoving);

  // Synchronizacja współrzędnych wizualnych
  useEffect(() => {
    if (!isMoving) {
      setVisualCoords({ row, col });
    }
  }, [row, col, isMoving]);

  // Główna logika animacji
  useEffect(() => {
    const oldPos = prevPositionRef.current;

    if (oldPos !== position) {
      console.log(
        `%c[PAWN LOG] ${color}-${id} position changed: ${oldPos} -> ${position}`,
        "color: yellow"
      );
      prevPositionRef.current = position;

      // SCENARIUSZ A: Bicie (Capture)
      if (oldPos >= 0 && position === -1) {
        console.log(
          "%c[ANIM] Triggering Capture Animation",
          "color: red; font-weight: bold"
        );
        const targetCoords = getPawnCoords(-1, 0, color as Color, id);
        handleCapturedAnimation(targetCoords);
      }
      // SCENARIUSZ B: Wyjście z bazy
      else if (oldPos === -1 && position !== -1) {
        controls.set({ scale: 1, opacity: 1, rotate: 0 });
        console.log("Getting out of base..., new coords:", { row, col });
        setVisualCoords({ row, col });
      }
      // SCENARIUSZ C: Ruch po planszy
      else if (position !== -1 && oldPos !== -1) {
        const steps = diceValue || 0;
        if (steps > 0) {
          handleAnimateSequence(oldPos, steps);
        } else {
          controls.set({ gridRow: row, gridColumn: col });
        }
      }
    }
  }, [position]); // Skrócona tablica zależności dla stabilności

  const handleCapturedAnimation = async (target: {
    row: number;
    col: number;
  }) => {
    setIsMoving(true);
    console.log("[ANIM] Captured animation started...");
    try {
      // 1. Reakcja na bicie
      await controls.start({
        scale: [1, 1.4, 0.5],
        rotate: [0, 180],
        transition: { duration: 0.3 },
      });
      // 2. Przelot
      await controls.start({
        gridRow: target.row,
        gridColumn: target.col,
        rotate: 360,
        transition: { duration: 0.6, ease: "anticipate" },
      });
      // 3. Lądowanie
      await controls.start({
        scale: [0.5, 1.1, 1],
        rotate: 0,
        transition: { duration: 0.2 },
      });
    } finally {
      setIsMoving(false);
      console.log("[ANIM] Captured animation finished.");
    }
  };

  const handleAnimateSequence = async (startPos: number, steps: number) => {
    setIsMoving(true);
    const path = getPathCoords(startPos, steps, color as Color, id, stepsMoved);
    try {
      for (const step of path) {
        await controls.start({
          gridRow: step.row,
          gridColumn: step.col,
          y: [0, -20, 0],
          transition: { duration: 0.2 },
        });
      }
    } finally {
      setIsMoving(false);
      if (onMoveComplete) onMoveComplete(id);
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
        gridRow: visualCoords.row,
        gridColumn: visualCoords.col,
        zIndex: isMoving ? 100 : activeInteractable ? 35 : 20,
      }}
      onClick={() => {
        if (activeInteractable && onClick) {
          console.log(`[CLICK] Pawn ${color}-${id} clicked`);
          onClick(id);
        }
      }}
      whileHover={activeInteractable ? { scale: 1.1 } : {}}
      className={`
        relative w-[85%] h-[85%] rounded-full m-auto
        border-2 border-white/60 shadow-xl flex items-center justify-center
        transition-all duration-300
        ${
          isMoving
            ? "cursor-default"
            : activeInteractable
            ? "cursor-pointer"
            : "cursor-not-allowed"
        }
        ${colorConfig[color as Color]}
        ${activeInteractable ? "ring-4 ring-white shadow-[0_0_20px_white]" : ""}
      `}
    >
      <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-black/20 via-transparent to-white/30" />

      {activeInteractable && (
        <>
          <motion.div
            layoutId={`pulse-${color}-${id}`}
            className="absolute -inset-2 rounded-full border-2 border-white/50"
            animate={{ scale: [1, 1.3], opacity: [0.5, 0] }}
            transition={{ repeat: Infinity, duration: 1 }}
          />
          {diceValue && (
            <motion.div
              initial={{ opacity: 0, y: 0 }}
              animate={{ opacity: 1, y: -35 }}
              className="absolute whitespace-nowrap bg-white text-slate-900 text-[10px] px-2 py-0.5 rounded-md font-black shadow-xl border border-white z-[200]"
            >
              {diceValue}
            </motion.div>
          )}
        </>
      )}
    </motion.div>
  );
}
