import { motion, AnimatePresence } from "framer-motion";
import { useState, useEffect } from "react";

const DiceFace = ({ value }: { value: number }) => (
  <div className="grid grid-cols-3 grid-rows-3 w-20 h-20 p-4 bg-white rounded-2xl shadow-[inset_0_0_15px_rgba(0,0,0,0.1)]">
    {value === 1 && (
      <div className="col-start-2 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
    )}
    {value === 2 && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {value === 3 && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-2 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {value === 4 && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-1 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {value === 5 && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-2 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-1 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
    {value === 6 && (
      <>
        <div className="col-start-1 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-1 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-1 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-2 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-1 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
        <div className="col-start-3 row-start-3 w-4 h-4 bg-slate-900 rounded-full" />
      </>
    )}
  </div>
);

export function DicePopup({
  isOpen,
  onClose,
  onRollComplete,
}: {
  isOpen: boolean;
  onClose: () => void;
  onRollComplete: (v: number) => void;
}) {
  const [isRolling, setIsRolling] = useState(false);
  const [result, setResult] = useState<number | null>(null);

  useEffect(() => {
    if (isOpen) {
      setResult(null);
      setIsRolling(false);
    }
  }, [isOpen]);

  useEffect(() => {
    let interval: any;
    if (isRolling) {
      interval = setInterval(() => {}, 80);
    }
    return () => clearInterval(interval);
  }, [isRolling]);

  const handleRoll = () => {
    setIsRolling(true);
    setResult(null);
    setTimeout(() => {
      const finalValue = Math.floor(Math.random() * 6) + 1;
      setResult(finalValue);
      setIsRolling(false);
      onRollComplete(finalValue);
    }, 1000);
  };

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
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0.8, opacity: 0 }}
            className="relative w-80 bg-[#121018] border border-white/10 rounded-[45px] p-10 shadow-2xl flex flex-col items-center"
          >
            <div className="h-44 flex items-center justify-center relative">
              <motion.div
                animate={
                  isRolling
                    ? {
                        rotate: [0, 90, 180, 270, 360],
                        scale: [1, 1.1, 1, 1.1, 1],
                      }
                    : {
                        rotate: 0,
                        scale: result ? [0.8, 1.15, 1] : 1,
                      }
                }
                transition={{ duration: 0.3, repeat: isRolling ? Infinity : 0 }}
              >
                {result && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.5 }}
                    animate={{ opacity: 1, scale: 1.8 }}
                    className="absolute inset-0  blur-3xl rounded-full"
                  />
                )}
                <DiceFace value={result !== null ? result : 6} />
              </motion.div>
            </div>

            <div className="text-center w-full mt-4">
              <h2 className="text-white text-xl font-black italic uppercase tracking-widest mb-8">
                {isRolling
                  ? "Rolling..."
                  : result
                  ? `Result: ${result}`
                  : "Dice Roll"}
              </h2>

              <button
                onClick={result !== null ? onClose : handleRoll}
                disabled={isRolling}
                className={`
                  w-full py-4 rounded-2xl font-black uppercase tracking-widest transition-all shadow-lg active:scale-95
                  ${
                    result !== null
                      ? "bg-purple-600 text-white hover:bg-purple-500"
                      : "bg-purple-600 text-white hover:bg-purple-500"
                  }
                  ${isRolling ? "opacity-30 cursor-wait" : "opacity-100"}
                `}
              >
                {isRolling
                  ? "Wait..."
                  : result !== null
                  ? "Okay"
                  : "Throw Dice"}
              </button>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
