import { motion, AnimatePresence } from "framer-motion";
import { useState, useEffect } from "react";
import { useLudo } from "../../../context/LudoGameContext";

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
}: {
  isOpen: boolean;
  onClose: () => void;
}) {
  const { gameState, isRolling, rollDice, setGameNotification } = useLudo();
  const [displayValue, setDisplayValue] = useState(1);
  const [isVisuallyRolling, setIsVisuallyRolling] = useState(false);

  // 1. Bezpieczne ustawianie powiadomienia o rozpoczęciu rzutu
  useEffect(() => {
    if (isOpen && !gameState?.diceRolled) {
      setGameNotification(
        `Player ${gameState?.currentPlayerColor || "Unit"} preparing dice...`,
        "INFO"
      );
    }
  }, [
    isOpen,
    gameState?.diceRolled,
    setGameNotification,
    gameState?.currentPlayerColor,
  ]);

  // 2. Obsługa wizualnego kręcenia (sztuczne wydłużenie dla efektu)
  useEffect(() => {
    if (isRolling) {
      setIsVisuallyRolling(true);
      setGameNotification(
        `Player ${gameState?.currentPlayerColor || "Unit"} is rolling...`,
        "ROLLING"
      );
    } else {
      const timer = setTimeout(() => {
        setIsVisuallyRolling(false);
      }, 800);
      return () => clearTimeout(timer);
    }
  }, [isRolling, setGameNotification, gameState?.currentPlayerColor]);

  // 3. Powiadomienie o finalnym wyniku po zatrzymaniu kostki
  useEffect(() => {
    if (
      !isVisuallyRolling &&
      !isRolling &&
      gameState?.diceRolled &&
      gameState.lastDiceRoll > 0
    ) {
      setGameNotification(
        `Player ${gameState?.currentPlayerColor || "Unit"} rolled a ${
          gameState.lastDiceRoll
        }!`,
        "ROLLED"
      );
    }
  }, [
    isVisuallyRolling,
    isRolling,
    gameState?.diceRolled,
    gameState?.lastDiceRoll,
    setGameNotification,
    gameState?.currentPlayerColor,
  ]);

  // 4. Interwał losowych wartości na kostce
  useEffect(() => {
    let interval: any;
    if (isVisuallyRolling || isRolling) {
      interval = setInterval(() => {
        setDisplayValue(Math.floor(Math.random() * 6) + 1);
      }, 100);
    } else if (gameState?.lastDiceRoll) {
      setDisplayValue(gameState.lastDiceRoll);
    }
    return () => clearInterval(interval);
  }, [isVisuallyRolling, isRolling, gameState?.lastDiceRoll]);

  const isFinished =
    !isVisuallyRolling &&
    !isRolling &&
    gameState?.diceRolled &&
    gameState.lastDiceRoll > 0;

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
              {isFinished && (
                <motion.div
                  initial={{ opacity: 0, scale: 0.5 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="absolute -top-6 text-7xl font-black text-purple-500/40 italic"
                >
                  {gameState.lastDiceRoll}
                </motion.div>
              )}

              <motion.div
                animate={
                  isVisuallyRolling || isRolling
                    ? {
                        rotate: [0, 90, 180, 270, 360],
                        scale: [1, 1.2, 1],
                        x: [0, -2, 2, -2, 0],
                      }
                    : { rotate: 0 }
                }
                transition={
                  isVisuallyRolling || isRolling
                    ? { repeat: Infinity, duration: 0.3, ease: "linear" }
                    : { type: "spring", stiffness: 260, damping: 20 }
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

            <div className="text-center w-full mt-4 min-h-[120px] flex flex-col justify-center">
              <h2 className="text-white text-xl font-black italic uppercase tracking-widest">
                {isVisuallyRolling || isRolling
                  ? "Moving on..."
                  : isFinished
                  ? `Result: ${gameState?.lastDiceRoll}`
                  : "Dice Ready"}
              </h2>

              <div className="mt-8 flex justify-center items-center min-h-[60px]">
                {/* PRZYCISK ZNIKA JEŚLI TRWA RZUT LUB JEST JUŻ WYNIK */}
                {!isRolling && !isVisuallyRolling && !gameState?.diceRolled ? (
                  <button
                    onClick={rollDice}
                    className="w-full py-4 rounded-2xl font-black uppercase tracking-widest transition-all shadow-lg bg-purple-600 hover:bg-purple-500 shadow-purple-500/20 active:scale-95"
                  >
                    Initialize Roll
                  </button>
                ) : (
                  <div className="flex flex-col items-center gap-2">
                    {isFinished ? (
                      <motion.p
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="text-emerald-500 font-bold text-sm tracking-tight uppercase"
                      >
                        Syncing Board...
                      </motion.p>
                    ) : (
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{
                          repeat: Infinity,
                          duration: 1,
                          ease: "linear",
                        }}
                        className="w-8 h-8 border-2 border-purple-500 border-t-transparent rounded-full"
                      />
                    )}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
