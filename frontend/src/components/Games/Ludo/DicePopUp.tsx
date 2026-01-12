import { motion, AnimatePresence } from "framer-motion";
import { useState, useEffect } from "react";
import { useLudo } from "../../../context/LudoGameContext"; // Importuj swój context

// DiceFace pozostaje bez zmian (ten sam kod z kropkami)
const DiceFace = ({ value }: { value: number }) => (
  <div className="grid grid-cols-3 grid-rows-3 w-20 h-20 p-4 bg-white rounded-2xl shadow-[inset_0_0_15px_rgba(0,0,0,0.1)]">
    {(value === 1 || value === 3 || value === 5) && (
      <div className="col-start-2 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
    )}
    {(value === 2 ||
      value === 3 ||
      value === 4 ||
      value === 5 ||
      value === 6) && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {(value === 4 || value === 5 || value === 6) && (
      <>
        <div className="col-start-3 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-1 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {value === 6 && (
      <>
        <div className="col-start-1 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
  </div>
);

export function DicePopup({
  isOpen,
  onClose,
}: {
  isOpen: boolean;
  onClose: () => void;
}) {
  const { gameState, isRolling, rollDice } = useLudo();
  const [displayValue, setDisplayValue] = useState(1);

  // Efekt "shuffling" - kręci się tylko gdy context mówi, że isRolling === true
  useEffect(() => {
    let interval: any;
    if (isRolling) {
      interval = setInterval(() => {
        setDisplayValue(Math.floor(Math.random() * 6) + 1);
      }, 100);
    } else if (gameState?.lastDiceRoll) {
      // Gdy rzut się zakończy, ustawiamy finalną wartość z serwera
      setDisplayValue(gameState.lastDiceRoll);
    }
    return () => clearInterval(interval);
  }, [isRolling, gameState?.lastDiceRoll]);

  // Gdy rzut jest gotowy (diceRolled to true i już się nie kręci)
  const isFinished =
    !isRolling && gameState?.diceRolled && gameState.lastDiceRoll > 0;

  return (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-[999] flex items-center justify-center bg-black/80 backdrop-blur-md"
        >
          <motion.div
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            className="relative w-80 bg-[#121018] border border-white/10 rounded-[45px] p-10 shadow-2xl flex flex-col items-center"
          >
            <div className="h-44 flex items-center justify-center relative">
              <motion.div
                animate={
                  isRolling
                    ? { rotate: [0, 90, 180, 270, 360], scale: [1, 1.1, 1] }
                    : { rotate: 0 }
                }
                transition={
                  isRolling
                    ? { repeat: Infinity, duration: 0.4 }
                    : { duration: 0.3 }
                }
              >
                {isFinished && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.5 }}
                    animate={{ opacity: 1, scale: 2 }}
                    className="absolute inset-0 bg-purple-500/20 blur-3xl rounded-full -z-10"
                  />
                )}
                <DiceFace value={displayValue} />
              </motion.div>
            </div>

            <div className="text-center w-full mt-4">
              <h2 className="text-white text-xl font-black italic uppercase tracking-widest mb-8">
                {isRolling
                  ? "Neural Sync..."
                  : isFinished
                  ? `Result: ${gameState?.lastDiceRoll}`
                  : "Dice Protocol"}
              </h2>

              <button
                onClick={isFinished ? onClose : rollDice}
                disabled={isRolling}
                className={`w-full py-4 rounded-2xl font-black uppercase tracking-widest transition-all shadow-lg
                  ${
                    isFinished
                      ? "bg-emerald-600 hover:bg-emerald-500"
                      : "bg-purple-600 hover:bg-purple-500"
                  }
                  ${
                    isRolling
                      ? "opacity-30 cursor-wait"
                      : "opacity-100 active:scale-95"
                  }
                `}
              >
                {isRolling
                  ? "Waiting..."
                  : isFinished
                  ? "Apply & Continue"
                  : "Initialize Roll"}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
