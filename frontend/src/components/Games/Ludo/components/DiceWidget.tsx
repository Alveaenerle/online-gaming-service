import { motion } from "framer-motion";
import { useState, useEffect } from "react";
import { Dices, Loader2 } from "lucide-react";
import { useLudo } from "../../../../context/LudoGameContext";

// Dice face component showing dots
const DiceFace = ({ value }: { value: number }) => {
  const dotPositions: Record<number, string[]> = {
    1: ["col-start-2 row-start-2"],
    2: ["col-start-1 row-start-1", "col-start-3 row-start-3"],
    3: ["col-start-1 row-start-1", "col-start-2 row-start-2", "col-start-3 row-start-3"],
    4: ["col-start-1 row-start-1", "col-start-3 row-start-1", "col-start-1 row-start-3", "col-start-3 row-start-3"],
    5: ["col-start-1 row-start-1", "col-start-3 row-start-1", "col-start-2 row-start-2", "col-start-1 row-start-3", "col-start-3 row-start-3"],
    6: ["col-start-1 row-start-1", "col-start-3 row-start-1", "col-start-1 row-start-2", "col-start-3 row-start-2", "col-start-1 row-start-3", "col-start-3 row-start-3"],
  };

  return (
    <div className="grid grid-cols-3 grid-rows-3 w-16 h-16 p-3 bg-white rounded-xl shadow-[inset_0_2px_10px_rgba(0,0,0,0.1)]">
      {dotPositions[value]?.map((pos, i) => (
        <div key={i} className={`${pos} w-3 h-3 bg-slate-900 rounded-full`} />
      ))}
    </div>
  );
};

interface DiceWidgetProps {
  isMyTurn: boolean;
  canRoll: boolean;
}

/**
 * Sidebar dice widget that replaces the modal popup.
 * Shows dice state and allows rolling when it's the player's turn.
 */
export function DiceWidget({ isMyTurn, canRoll }: DiceWidgetProps) {
  const { gameState, isRolling, rollDice } = useLudo();
  const [displayValue, setDisplayValue] = useState(1);
  const [isVisuallyRolling, setIsVisuallyRolling] = useState(false);

  // Handle visual rolling effect (extended animation)
  useEffect(() => {
    if (isRolling) {
      setIsVisuallyRolling(true);
    } else {
      const timer = setTimeout(() => {
        setIsVisuallyRolling(false);
      }, 800);
      return () => clearTimeout(timer);
    }
  }, [isRolling]);

  // Animate dice face during rolling
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isVisuallyRolling || isRolling) {
      interval = setInterval(() => {
        setDisplayValue(Math.floor(Math.random() * 6) + 1);
      }, 100);
    } else if (gameState?.lastDiceRoll) {
      setDisplayValue(gameState.lastDiceRoll);
    }
    return () => clearInterval(interval);
  }, [isVisuallyRolling, isRolling, gameState?.lastDiceRoll]);

  const isFinished = !isVisuallyRolling && !isRolling && gameState?.diceRolled && gameState.lastDiceRoll > 0;
  const showRollButton = isMyTurn && canRoll && !isRolling && !isVisuallyRolling && !gameState?.diceRolled;

  return (
    <div className="bg-[#121018]/80 backdrop-blur-xl rounded-2xl border border-white/10 p-4">
      <div className="flex items-center gap-2 mb-4">
        <Dices size={14} className="text-purple-500" />
        <h3 className="text-[10px] font-black uppercase tracking-[0.2em] text-white/60">
          Dice Control
        </h3>
      </div>

      <div className="flex flex-col items-center">
        {/* Dice Display */}
        <div className="relative mb-4">
          {/* Glow effect when it's player's turn */}
          {isMyTurn && !gameState?.diceRolled && (
            <div className="absolute inset-0 bg-purple-500/20 blur-xl rounded-full animate-pulse" />
          )}

          <motion.div
            animate={
              isVisuallyRolling || isRolling
                ? {
                    rotate: [0, 90, 180, 270, 360],
                    scale: [1, 1.1, 1],
                  }
                : { rotate: 0, scale: 1 }
            }
            transition={
              isVisuallyRolling || isRolling
                ? { repeat: Infinity, duration: 0.3, ease: "linear" }
                : { type: "spring", stiffness: 260, damping: 20 }
            }
            className="relative z-10"
          >
            <DiceFace value={displayValue} />
          </motion.div>

          {/* Result indicator */}
          {isFinished && (
            <motion.div
              initial={{ opacity: 0, scale: 0.5 }}
              animate={{ opacity: 1, scale: 1 }}
              className="absolute -top-2 -right-2 w-7 h-7 rounded-full bg-purple-500 flex items-center justify-center text-white text-xs font-black shadow-lg shadow-purple-500/50"
            >
              {gameState?.lastDiceRoll}
            </motion.div>
          )}
        </div>

        {/* Status Text */}
        <div className="text-center mb-4">
          <p className="text-xs text-gray-400 font-medium">
            {isVisuallyRolling || isRolling ? (
              <span className="text-purple-400 animate-pulse">Rolling...</span>
            ) : isFinished ? (
              <span className="text-emerald-400">
                You rolled a <span className="font-black text-lg">{gameState?.lastDiceRoll}</span>
              </span>
            ) : isMyTurn && canRoll ? (
              <span className="text-purple-400">Your turn to roll!</span>
            ) : isMyTurn && gameState?.waitingForMove ? (
              <span className="text-amber-400">Select a pawn to move</span>
            ) : (
              <span className="text-gray-500">Waiting for turn...</span>
            )}
          </p>
          {gameState?.rollsLeft !== undefined && gameState.rollsLeft > 0 && isMyTurn && (
            <p className="text-[10px] text-gray-500 mt-1">
              Rolls remaining: <span className="text-purple-400 font-bold">{gameState.rollsLeft}</span>
            </p>
          )}
        </div>

        {/* Roll Button */}
        {showRollButton ? (
          <motion.button
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={rollDice}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-600 to-purple-500 text-white font-black uppercase tracking-widest text-sm shadow-lg shadow-purple-500/30 hover:shadow-purple-500/50 transition-all"
          >
            Roll Dice
          </motion.button>
        ) : isRolling || isVisuallyRolling ? (
          <div className="flex items-center justify-center gap-2 py-3 text-purple-400">
            <Loader2 size={16} className="animate-spin" />
            <span className="text-xs font-bold uppercase tracking-wider">Rolling...</span>
          </div>
        ) : gameState?.waitingForMove && isMyTurn ? (
          <div className="w-full py-3 rounded-xl bg-amber-500/20 border border-amber-500/30 text-amber-400 text-center text-xs font-bold uppercase tracking-wider">
            Move a Pawn
          </div>
        ) : (
          <div className="w-full py-3 rounded-xl bg-white/5 text-gray-500 text-center text-xs font-bold uppercase tracking-wider">
            {isMyTurn ? "Wait..." : "Not Your Turn"}
          </div>
        )}
      </div>
    </div>
  );
}

export default DiceWidget;
